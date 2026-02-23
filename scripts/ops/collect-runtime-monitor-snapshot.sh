#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"
cd "$project_abs"

compose_file="${OPS_COMPOSE_FILE:-$project_abs/docker-compose.yml}"
compose_project="${OPS_COMPOSE_PROJECT:-tcs}"
tail_lines="${OPS_LOG_TAIL_LINES:-2000}"
app_service="${OPS_APP_SERVICE:-app}"

tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/ops}"
report_file="${OPS_MONITOR_REPORT_FILE:-$tmp_root/latest/runtime-monitor-snapshot-latest.md}"
mkdir -p "$(dirname "$report_file")"

log_source="none"
logs=""
if docker-compose -f "$compose_file" -p "$compose_project" ps -q "$app_service" >/dev/null 2>&1; then
  app_cid="$(docker-compose -f "$compose_file" -p "$compose_project" ps -q "$app_service" | head -n 1 || true)"
  if [[ -n "${app_cid:-}" ]]; then
    logs="$(docker-compose -f "$compose_file" -p "$compose_project" logs --no-color --tail "$tail_lines" "$app_service" 2>/dev/null || true)"
    log_source="docker-compose:${compose_project}/${app_service}"
  fi
fi

count_pattern() {
  local pattern="$1"
  if [[ -z "$logs" ]]; then
    echo "0"
  else
    printf "%s\n" "$logs" | grep -c "$pattern" || true
  fi
}

auth_monitor_count="$(count_pattern 'AUTH_MONITOR')"
queue_monitor_count="$(count_pattern 'QUEUE_MONITOR')"
push_monitor_count="$(count_pattern 'PUSH_MONITOR')"
push_snapshot_count="$(count_pattern 'PUSH_MONITOR_SNAPSHOT')"

cat >"$report_file" <<EOF
# Runtime Monitor Snapshot Report

- Log Source: \`${log_source}\`
- Tail Lines: \`${tail_lines}\`

## Monitor Key Counts

| Key | Count |
| --- | --- |
| AUTH_MONITOR | ${auth_monitor_count} |
| QUEUE_MONITOR | ${queue_monitor_count} |
| PUSH_MONITOR | ${push_monitor_count} |
| PUSH_MONITOR_SNAPSHOT | ${push_snapshot_count} |
EOF

echo "[ops-monitor] report: ${report_file#$repo_root/}"
