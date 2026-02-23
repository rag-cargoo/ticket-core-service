#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"
cd "$project_abs"

compose_file="${IT_COMPOSE_FILE:-$project_abs/docker-compose.yml}"
compose_project="${IT_COMPOSE_PROJECT:-tcsit}"
app_replicas="${IT_APP_REPLICAS:-1}"
keep_env="${IT_KEEP_ENV:-false}"
health_url="${IT_HEALTH_URL:-http://127.0.0.1:18080/api/concerts}"
health_attempts="${IT_HEALTH_ATTEMPTS:-60}"
health_sleep_sec="${IT_HEALTH_SLEEP_SEC:-2}"

run_stamp="${IT_RUN_STAMP:-$(date -u '+%Y%m%dT%H%M%SZ')}"
tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/integration}"
run_dir="$tmp_root/$run_stamp"
log_file="${IT_LOG_FILE:-$run_dir/runtime-integration.log}"
report_file="${IT_REPORT_FILE:-$tmp_root/latest/runtime-integration-latest.md}"

mkdir -p "$(dirname "$report_file")" "$(dirname "$log_file")"

cleanup() {
  if [[ "$keep_env" != "true" ]]; then
    docker-compose -f "$compose_file" -p "$compose_project" down -v >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "[runtime-it] compose up: project=$compose_project replicas=$app_replicas"
docker-compose -f "$compose_file" -p "$compose_project" up -d --build --scale app="$app_replicas" postgres-db redis zookeeper kafka app nginx-lb

echo "[runtime-it] health check: $health_url"
healthy="false"
for _ in $(seq 1 "$health_attempts"); do
  code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 3 "$health_url" || true)"
  if [[ "$code" =~ ^2[0-9][0-9]$ ]]; then
    healthy="true"
    break
  fi
  sleep "$health_sleep_sec"
done

if [[ "$healthy" != "true" ]]; then
  echo "[runtime-it] backend health check failed: $health_url"
  exit 1
fi

set +e
./gradlew --no-daemon test \
  --tests 'com.ticketrush.application.concert.service.ConcertExplorerIntegrationTest' \
  --tests 'com.ticketrush.application.reservation.service.ReservationLifecycleServiceIntegrationTest' \
  --tests 'com.ticketrush.infrastructure.messaging.KafkaPushEventConsumerTest' \
  2>&1 | tee "$log_file"
test_rc=${PIPESTATUS[0]}
set -e

result="PASS"
if [[ "$test_rc" -ne 0 ]]; then
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

log_display="$(to_repo_relative "$log_file")"
report_display="$(to_repo_relative "$report_file")"

cat >"$report_file" <<EOF
# Runtime Integration Test Report

- Result: ${result}
- Compose Project: \`${compose_project}\`
- Compose File: \`${compose_file}\`
- App Replicas: \`${app_replicas}\`
- Health URL: \`${health_url}\`
- Keep Environment: \`${keep_env}\`

## Executed Tests

- \`com.ticketrush.application.concert.service.ConcertExplorerIntegrationTest\`
- \`com.ticketrush.application.reservation.service.ReservationLifecycleServiceIntegrationTest\`
- \`com.ticketrush.infrastructure.messaging.KafkaPushEventConsumerTest\`

## Artifacts

- runtime integration log: \`${log_display}\`
EOF

echo "[runtime-it] report: $report_display"
exit "$test_rc"
