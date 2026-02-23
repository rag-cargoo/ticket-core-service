#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"

api_url="${OPS_HEALTH_API_URL:-http://127.0.0.1:8080/api/concerts}"
redis_host="${OPS_REDIS_HOST:-127.0.0.1}"
redis_port="${OPS_REDIS_PORT:-6379}"
kafka_host="${OPS_KAFKA_HOST:-127.0.0.1}"
kafka_port="${OPS_KAFKA_PORT:-9092}"
postgres_host="${OPS_POSTGRES_HOST:-127.0.0.1}"
postgres_port="${OPS_POSTGRES_PORT:-5432}"

tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/ops}"
report_file="${OPS_HEALTH_REPORT_FILE:-$tmp_root/latest/runtime-health-check-latest.md}"
mkdir -p "$(dirname "$report_file")"

http_code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 3 "$api_url" || true)"
if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
  api_status="UP(${http_code})"
else
  api_status="DOWN(${http_code:-N/A})"
fi

check_tcp() {
  local host="$1"
  local port="$2"
  if timeout 2 bash -c "cat < /dev/null > /dev/tcp/${host}/${port}" 2>/dev/null; then
    echo "UP"
  else
    echo "DOWN"
  fi
}

redis_status="$(check_tcp "$redis_host" "$redis_port")"
kafka_status="$(check_tcp "$kafka_host" "$kafka_port")"
postgres_status="$(check_tcp "$postgres_host" "$postgres_port")"

overall="PASS"
if [[ "$api_status" != UP* ]] || [[ "$redis_status" != "UP" ]] || [[ "$kafka_status" != "UP" ]] || [[ "$postgres_status" != "UP" ]]; then
  overall="FAIL"
fi

cat >"$report_file" <<EOF
# Runtime Health Check Report

- Result: ${overall}
- API URL: \`${api_url}\`

## Status

| Component | Status |
| --- | --- |
| API | ${api_status} |
| Redis (${redis_host}:${redis_port}) | ${redis_status} |
| Kafka (${kafka_host}:${kafka_port}) | ${kafka_status} |
| Postgres (${postgres_host}:${postgres_port}) | ${postgres_status} |
EOF

echo "[ops-health] report: ${report_file#$repo_root/}"
if [[ "$overall" == "FAIL" ]]; then
  exit 1
fi
