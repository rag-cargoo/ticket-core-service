#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"

compose_file="${DIST_COMPOSE_FILE:-$project_abs/docker-compose.yml}"
compose_project="${DIST_COMPOSE_PROJECT:-tcsdist}"
lb_service="${DIST_LB_SERVICE_NAME:-nginx-lb}"
docker_network="${DIST_DOCKER_NETWORK:-${compose_project}_ticket-network}"
keep_env="${DIST_KEEP_ENV:-false}"
app_replicas="${DIST_APP_REPLICAS:-3}"

run_stamp="${K6_RUN_STAMP:-$(date -u '+%Y%m%dT%H%M%SZ')}"
tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/k6/distributed}"
latest_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/k6/latest}"
run_dir="$tmp_root/$run_stamp"

summary_json_host="${K6_SUMMARY_JSON:-$latest_root/k6-distributed-summary.json}"
report_file="${K6_REPORT_FILE:-$latest_root/k6-distributed-latest.md}"
log_file="${K6_LOG_FILE:-$run_dir/k6-distributed.log}"

api_host="${API_HOST:-http://${lb_service}:8080}"
health_url="${K6_HEALTH_URL:-http://${lb_service}:8080/api/concerts}"
host_api_host="${DIST_HOST_API_HOST:-http://127.0.0.1:18080}"
concert_id="${CONCERT_ID:-1}"
vus="${K6_VUS:-60}"
duration="${K6_DURATION:-60s}"
sleep_max_sec="${K6_SLEEP_MAX_SEC:-0.2}"
user_id_base="${K6_USER_ID_BASE:-910000000}"
docker_user="${K6_DOCKER_USER:-$(id -u):$(id -g)}"
k6_image="${K6_DOCKER_IMAGE:-grafana/k6:latest}"
warmup_max_attempts="${K6_WARMUP_MAX_ATTEMPTS:-60}"
warmup_consecutive="${K6_WARMUP_CONSECUTIVE:-5}"
warmup_sleep_seconds="${K6_WARMUP_SLEEP_SECONDS:-1}"
warmup_burst_requests="${K6_WARMUP_BURST_REQUESTS:-30}"
warmup_burst_attempts="${K6_WARMUP_BURST_ATTEMPTS:-10}"

mkdir -p "$(dirname "$summary_json_host")" "$(dirname "$report_file")" "$(dirname "$log_file")"
rm -f "$summary_json_host"

cleanup() {
  if [[ "$keep_env" != "true" ]]; then
    docker-compose -f "$compose_file" -p "$compose_project" down -v >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

warmup_join_once() {
  local probe_user_id="$1"
  local payload response code body status

  payload="$(printf '{"userId":%s,"concertId":%s}' "$probe_user_id" "$concert_id")"
  response="$(curl -sS --connect-timeout 2 --max-time 4 -w $'\n%{http_code}' \
    -H "Content-Type: application/json" \
    -X POST "${host_api_host}/api/v1/waiting-queue/join" \
    -d "$payload" || true)"
  code="$(echo "$response" | tail -n1)"
  if [[ "$code" != "200" ]]; then
    return 1
  fi

  body="$(echo "$response" | sed '$d')"
  status="$(echo "$body" | jq -r '.status // empty' 2>/dev/null || true)"
  [[ "$status" == "WAITING" || "$status" == "ACTIVE" || "$status" == "REJECTED" ]]
}

warmup_join_burst_once() {
  local batch_no="$1"
  local result_file total_count fail_count

  result_file="$(mktemp)"
  for req_no in $(seq 1 "$warmup_burst_requests"); do
    (
      probe_user_id=$((user_id_base + 200000000 + batch_no * 100000 + req_no))
      payload="$(printf '{"userId":%s,"concertId":%s}' "$probe_user_id" "$concert_id")"
      response="$(curl -sS --connect-timeout 2 --max-time 4 -w $'\n%{http_code}' \
        -H "Content-Type: application/json" \
        -X POST "${host_api_host}/api/v1/waiting-queue/join" \
        -d "$payload" || true)"
      code="$(echo "$response" | tail -n1)"
      if [[ "$code" != "200" ]]; then
        echo "fail" >>"$result_file"
        exit 0
      fi
      body="$(echo "$response" | sed '$d')"
      status="$(echo "$body" | jq -r '.status // empty' 2>/dev/null || true)"
      if [[ "$status" == "WAITING" || "$status" == "ACTIVE" || "$status" == "REJECTED" ]]; then
        echo "ok" >>"$result_file"
      else
        echo "fail" >>"$result_file"
      fi
    ) &
  done
  wait

  total_count="$(wc -l <"$result_file" | tr -d '[:space:]')"
  fail_count="$(grep -c '^fail$' "$result_file" || true)"
  rm -f "$result_file"

  [[ "$total_count" -eq "$warmup_burst_requests" && "$fail_count" -eq 0 ]]
}

echo "[distributed-k6] compose_up project=$compose_project file=$compose_file scale_app=$app_replicas"
docker-compose -f "$compose_file" -p "$compose_project" up -d --build --scale app="$app_replicas"

lb_cid="$(docker-compose -f "$compose_file" -p "$compose_project" ps -q "$lb_service" || true)"
if [[ -z "$lb_cid" ]]; then
  echo "[distributed-k6] failed to resolve LB container id: $lb_service"
  exit 1
fi

echo "[distributed-k6] waiting for LB health: $health_url"
healthy="false"
for _ in $(seq 1 60); do
  if docker exec "$lb_cid" wget -q -O /dev/null "http://127.0.0.1:8080/api/concerts" >/dev/null 2>&1; then
    healthy="true"
    break
  fi
  sleep 2
done

if [[ "$healthy" != "true" ]]; then
  echo "[distributed-k6] LB health check timeout: $health_url"
  exit 1
fi

echo "[distributed-k6] warm-up join API via host: ${host_api_host}/api/v1/waiting-queue/join"
join_ready="false"
consecutive_ok=0
for attempt in $(seq 1 "$warmup_max_attempts"); do
  probe_user_id=$((user_id_base + 100000000 + attempt))
  if warmup_join_once "$probe_user_id"; then
    consecutive_ok=$((consecutive_ok + 1))
    if [[ "$consecutive_ok" -ge "$warmup_consecutive" ]]; then
      join_ready="true"
      break
    fi
  else
    consecutive_ok=0
  fi
  sleep "$warmup_sleep_seconds"
done

if [[ "$join_ready" != "true" ]]; then
  echo "[distributed-k6] join warm-up timeout: host=${host_api_host} attempts=${warmup_max_attempts} consecutive_required=${warmup_consecutive}"
  exit 1
fi

echo "[distributed-k6] warm-up burst check: requests=${warmup_burst_requests} attempts=${warmup_burst_attempts}"
burst_ready="false"
for attempt in $(seq 1 "$warmup_burst_attempts"); do
  if warmup_join_burst_once "$attempt"; then
    burst_ready="true"
    break
  fi
  sleep "$warmup_sleep_seconds"
done

if [[ "$burst_ready" != "true" ]]; then
  echo "[distributed-k6] join burst warm-up timeout: host=${host_api_host} burst_requests=${warmup_burst_requests} attempts=${warmup_burst_attempts}"
  exit 1
fi

echo "[distributed-k6] k6 start api_host=$api_host network=$docker_network vus=$vus duration=$duration"
summary_json_container="/repo/${summary_json_host#$repo_root/}"

set +e
docker run --rm \
  --network "$docker_network" \
  --user "$docker_user" \
  -v "$repo_root:/repo" \
  -w /repo \
  "$k6_image" run scripts/perf/k6-waiting-queue-join.js \
    --summary-export "$summary_json_container" \
    --summary-trend-stats "avg,min,med,max,p(90),p(95),p(99)" \
    -e API_HOST="$api_host" \
    -e CONCERT_ID="$concert_id" \
    -e VUS="$vus" \
    -e DURATION="$duration" \
    -e SLEEP_MAX_SEC="$sleep_max_sec" \
    -e USER_ID_BASE="$user_id_base" \
    2>&1 | tee "$log_file"
k6_rc=${PIPESTATUS[0]}
set -e

if [[ ! -s "$summary_json_host" ]]; then
  echo "[distributed-k6] missing summary artifact: $summary_json_host"
  exit 1
fi

metric_or_na() {
  local query="$1"
  jq -r "${query} // \"N/A\"" "$summary_json_host" 2>/dev/null || echo "N/A"
}

http_reqs="$(metric_or_na '.metrics.http_reqs.values.count // .metrics.http_reqs.count')"
http_failed_rate="$(metric_or_na '.metrics.http_req_failed.values.rate // .metrics.http_req_failed.value')"
http_p95_ms="$(metric_or_na '.metrics.http_req_duration.values."p(95)" // .metrics.http_req_duration."p(95)"')"
http_p99_ms="$(metric_or_na '.metrics.http_req_duration.values."p(99)" // .metrics.http_req_duration."p(99)"')"
checks_rate="$(metric_or_na '.metrics.checks.values.rate // .metrics.checks.value')"
join_accepted="$(metric_or_na '.metrics.join_accepted.values.count // .metrics.join_accepted.count')"
join_rejected="$(metric_or_na '.metrics.join_rejected.values.count // .metrics.join_rejected.count')"
join_valid_status_rate="$(metric_or_na '.metrics.join_valid_status_rate.values.rate // .metrics.join_valid_status_rate.value')"

result="PASS"
if [[ "$k6_rc" -ne 0 ]]; then
  result="FAIL"
fi

to_repo_relative() {
  local path="$1"
  if [[ "$path" == "$repo_root/"* ]]; then
    echo "${path#$repo_root/}"
  else
    echo "$path"
  fi
}

summary_display="$(to_repo_relative "$summary_json_host")"
report_display="$(to_repo_relative "$report_file")"
log_display="$(to_repo_relative "$log_file")"

cat >"$report_file" <<EOF
# k6 Distributed Waiting Queue Load Test Report

- Result: ${result}
- Run Stamp (UTC): ${run_stamp}
- Compose Project: \`${compose_project}\`
- App Replicas: \`${app_replicas}\`
- LB Service: \`${lb_service}\`
- API Host (in-network): \`${api_host}\`
- API Host (host access): \`${host_api_host}\`
- VUs: ${vus}
- Duration: ${duration}
- k6 Script: \`scripts/perf/k6-waiting-queue-join.js\`
- Keep Environment: \`${keep_env}\`

## Summary Metrics

| Metric | Value |
| --- | --- |
| http_reqs.count | ${http_reqs} |
| http_req_failed.rate | ${http_failed_rate} |
| http_req_duration.p(95) ms | ${http_p95_ms} |
| http_req_duration.p(99) ms | ${http_p99_ms} |
| checks.rate | ${checks_rate} |
| join_accepted.count | ${join_accepted} |
| join_rejected.count | ${join_rejected} |
| join_valid_status_rate.rate | ${join_valid_status_rate} |

## Artifacts

- report: \`${report_display}\`
- summary: \`${summary_display}\`
- log: \`${log_display}\`
EOF

echo "[distributed-k6] report: $report_display"
echo "[distributed-k6] summary: $summary_display"
echo "[distributed-k6] log: $log_display"

exit "$k6_rc"
