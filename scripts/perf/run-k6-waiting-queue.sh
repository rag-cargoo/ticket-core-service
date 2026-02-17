#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"
tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/k6}"
run_stamp="${K6_RUN_STAMP:-$(date -u '+%Y%m%dT%H%M%SZ')}"
run_tmp_dir="${tmp_root}/${run_stamp}"

k6_script="${K6_SCRIPT:-$project_abs/scripts/perf/k6-waiting-queue-join.js}"
report_file="${K6_REPORT_FILE:-$tmp_root/latest/k6-latest.md}"
log_file="${K6_LOG_FILE:-$run_tmp_dir/k6-latest.log}"
summary_json="${K6_SUMMARY_JSON:-$tmp_root/latest/k6-summary.json}"

api_host="${API_HOST:-http://127.0.0.1:8080}"
concert_id="${CONCERT_ID:-1}"
vus="${K6_VUS:-60}"
duration="${K6_DURATION:-60s}"
sleep_max_sec="${K6_SLEEP_MAX_SEC:-0.2}"
user_id_base="${K6_USER_ID_BASE:-900000000}"
dock_k6_image="${K6_DOCKER_IMAGE:-grafana/k6:latest}"
dock_network="${K6_DOCKER_NETWORK:-host}"
docker_user="${K6_DOCKER_USER:-$(id -u):$(id -g)}"
k6_web_dashboard="${K6_WEB_DASHBOARD:-false}"
k6_web_dashboard_host="${K6_WEB_DASHBOARD_HOST:-127.0.0.1}"
k6_web_dashboard_port="${K6_WEB_DASHBOARD_PORT:-5665}"
k6_web_dashboard_export="${K6_WEB_DASHBOARD_EXPORT:-$run_tmp_dir/k6-web-dashboard.html}"

health_url="${K6_HEALTH_URL:-${api_host}/api/concerts}"
run_started_utc="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
execution_mode="local"

if ! command -v jq >/dev/null 2>&1; then
  echo "[k6] command not found: jq"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[k6] command not found: curl"
  exit 1
fi

if [[ ! -f "$k6_script" ]]; then
  echo "[k6] script not found: $k6_script"
  exit 1
fi

health_code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 3 "$health_url" || true)"
if [[ ! "$health_code" =~ ^2[0-9][0-9]$ ]]; then
  echo "[k6] backend health check failed: $health_url (status: ${health_code:-N/A})"
  exit 1
fi

mkdir -p "$(dirname "$report_file")"
mkdir -p "$(dirname "$log_file")"
mkdir -p "$(dirname "$summary_json")"

# Prevent stale metrics from previous runs if current run is interrupted.
rm -f "$summary_json"

if [[ "$k6_web_dashboard" == "true" ]]; then
  mkdir -p "$(dirname "$k6_web_dashboard_export")"
fi

echo "[k6] start"
echo "[k6] api_host=$api_host concert_id=$concert_id vus=$vus duration=$duration"
echo "[k6] docker_image=$dock_k6_image docker_network=$dock_network"
echo "[k6] docker_user=$docker_user"
echo "[k6] web_dashboard=$k6_web_dashboard host=$k6_web_dashboard_host port=$k6_web_dashboard_port"
echo "[k6] tmp_run_dir=$run_tmp_dir"

require_path_under_repo() {
  local path="$1"
  if [[ "$path" != "$repo_root/"* ]]; then
    echo "[k6] path must be under repo root: $path"
    echo "[k6] repo root: $repo_root"
    exit 1
  fi
}

to_container_path() {
  local path="$1"
  echo "/repo/${path#$repo_root/}"
}

to_repo_relative() {
  local path="$1"
  if [[ "$path" == "$repo_root/"* ]]; then
    echo "${path#$repo_root/}"
  else
    echo "$path"
  fi
}

run_k6_local() {
  set +e
  K6_WEB_DASHBOARD="$k6_web_dashboard" \
  K6_WEB_DASHBOARD_HOST="$k6_web_dashboard_host" \
  K6_WEB_DASHBOARD_PORT="$k6_web_dashboard_port" \
  K6_WEB_DASHBOARD_EXPORT="$k6_web_dashboard_export" \
  k6 run "$k6_script" \
    --summary-export "$summary_json" \
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
}

run_k6_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "[k6] command not found: k6"
    echo "[k6] and docker is also not available for fallback"
    echo "[k6] install guide: https://grafana.com/docs/k6/latest/set-up/install-k6/"
    exit 1
  fi

  require_path_under_repo "$k6_script"
  require_path_under_repo "$summary_json"
  require_path_under_repo "$log_file"
  require_path_under_repo "$k6_web_dashboard_export"

  local k6_script_in_container
  local summary_in_container
  local dashboard_export_in_container
  k6_script_in_container="$(to_container_path "$k6_script")"
  summary_in_container="$(to_container_path "$summary_json")"
  dashboard_export_in_container="$(to_container_path "$k6_web_dashboard_export")"

  set +e
  docker run --rm \
    --network "$dock_network" \
    --user "$docker_user" \
    -v "$repo_root:/repo" \
    -w /repo \
    -e K6_WEB_DASHBOARD="$k6_web_dashboard" \
    -e K6_WEB_DASHBOARD_HOST="$k6_web_dashboard_host" \
    -e K6_WEB_DASHBOARD_PORT="$k6_web_dashboard_port" \
    -e K6_WEB_DASHBOARD_EXPORT="$dashboard_export_in_container" \
    "$dock_k6_image" run "$k6_script_in_container" \
      --summary-export "$summary_in_container" \
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
}

k6_rc=1
if command -v k6 >/dev/null 2>&1; then
  execution_mode="local"
  run_k6_local
else
  execution_mode="docker"
  echo "[k6] local k6 not found, switching to docker fallback"
  run_k6_docker
fi

metric_or_na() {
  local query="$1"
  if [[ ! -s "$summary_json" ]]; then
    echo "N/A"
    return 0
  fi

  jq -r "${query} // \"N/A\"" "$summary_json" 2>/dev/null || echo "N/A"
}

http_reqs="$(metric_or_na '.metrics.http_reqs.values.count // .metrics.http_reqs.count')"
http_failed_rate="$(metric_or_na '.metrics.http_req_failed.values.rate // .metrics.http_req_failed.value')"
http_p95_ms="$(metric_or_na '.metrics.http_req_duration.values."p(95)" // .metrics.http_req_duration."p(95)"')"
http_p99_ms="$(metric_or_na '.metrics.http_req_duration.values."p(99)" // .metrics.http_req_duration."p(99)"')"
checks_rate="$(metric_or_na '.metrics.checks.values.rate // .metrics.checks.value')"
join_accepted="$(metric_or_na '.metrics.join_accepted.values.count // .metrics.join_accepted.count')"
join_rejected="$(metric_or_na '.metrics.join_rejected.values.count // .metrics.join_rejected.count')"
join_unknown="$(metric_or_na '.metrics.join_unknown.values.count // .metrics.join_unknown.count')"
join_http_error="$(metric_or_na '.metrics.join_http_error.values.count // .metrics.join_http_error.count')"
join_valid_status_rate="$(metric_or_na '.metrics.join_valid_status_rate.values.rate // .metrics.join_valid_status_rate.value')"

result="PASS"
if [[ "$k6_rc" -ne 0 ]]; then
  result="FAIL"
fi

log_display="$(to_repo_relative "$log_file")"
summary_display="$(to_repo_relative "$summary_json")"
dashboard_display="$(to_repo_relative "$k6_web_dashboard_export")"
report_display="$(to_repo_relative "$report_file")"

cat >"$report_file" <<EOF
# k6 Waiting Queue Load Test Report

- Result: ${result}
- Run Started (UTC): ${run_started_utc}
- API Host: \`${api_host}\`
- Concert ID: ${concert_id}
- VUs: ${vus}
- Duration: ${duration}
- k6 Script: \`scripts/perf/k6-waiting-queue-join.js\`
- Execution Mode: \`${execution_mode}\`
- Web Dashboard Enabled: \`${k6_web_dashboard}\`
- Web Dashboard URL: \`http://${k6_web_dashboard_host}:${k6_web_dashboard_port}\`

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
| join_unknown.count | ${join_unknown} |
| join_http_error.count | ${join_http_error} |
| join_valid_status_rate.rate | ${join_valid_status_rate} |

## Artifacts

- k6 raw log: \`${log_display}\`
- k6 summary json: \`${summary_display}\`
- k6 web dashboard html: \`${dashboard_display}\`
EOF

if [[ "$k6_rc" -ne 0 ]]; then
  cat >>"$report_file" <<'EOF'

## Notes

- Threshold 미달 또는 실행 실패가 발생했습니다.
- 상세 원인은 `k6 raw log`를 확인하세요.
EOF
fi

echo "[k6] report: ${report_display}"
echo "[k6] summary: ${summary_display}"
echo "[k6] log: ${log_display}"

exit "$k6_rc"
