#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"

run_stamp="$(date -u '+%Y%m%dT%H%M%SZ')"
default_log_file="${repo_root}/.codex/tmp/ticket-core-service/step7/${run_stamp}/step7-regression.log"

compose_file="${STEP7_COMPOSE_FILE:-$project_abs/docker-compose.yml}"
health_url="${API_SCRIPT_HEALTH_URL:-http://127.0.0.1:8080/api/concerts}"
health_timeout_sec="${STEP7_HEALTH_TIMEOUT_SEC:-300}"
health_interval_sec="${STEP7_HEALTH_INTERVAL_SEC:-5}"
log_file="${STEP7_LOG_FILE:-$default_log_file}"
keep_env="${STEP7_KEEP_ENV:-false}"
compose_build="${STEP7_COMPOSE_BUILD:-true}"
force_recreate="${STEP7_FORCE_RECREATE:-true}"
step7_push_mode="${STEP7_PUSH_MODE:-sse}"

compose_cmd=()
if docker compose version >/dev/null 2>&1; then
  compose_cmd=(docker compose -f "$compose_file")
elif command -v docker-compose >/dev/null 2>&1; then
  compose_cmd=(docker-compose -f "$compose_file")
else
  echo "[step7-regression] docker compose command not found (docker compose | docker-compose)"
  exit 1
fi

cleanup() {
  set +e
  mkdir -p "$(dirname "$log_file")"
  "${compose_cmd[@]}" logs --no-color app redis kafka >"$log_file" 2>&1 || true
  if [[ "$keep_env" != "true" ]]; then
    "${compose_cmd[@]}" down || true
  fi
}
trap cleanup EXIT

if ! command -v docker >/dev/null 2>&1; then
  echo "[step7-regression] docker command not found"
  exit 1
fi

if [[ "$compose_build" == "true" ]]; then
  if [[ "$force_recreate" == "true" ]]; then
    echo "[step7-regression] compose down (force recreate)"
    "${compose_cmd[@]}" down || true
  fi
  echo "[step7-regression] compose up --build: postgres-db redis zookeeper kafka app (APP_PUSH_MODE=${step7_push_mode})"
  APP_PUSH_MODE="$step7_push_mode" "${compose_cmd[@]}" up --build -d postgres-db redis zookeeper kafka app
else
  echo "[step7-regression] compose up: postgres-db redis zookeeper kafka app (APP_PUSH_MODE=${step7_push_mode})"
  APP_PUSH_MODE="$step7_push_mode" "${compose_cmd[@]}" up -d postgres-db redis zookeeper kafka app
fi

echo "[step7-regression] waiting for backend health: $health_url"
start_ts="$(date +%s)"
while true; do
  code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 3 "$health_url" || true)"
  if [[ "$code" =~ ^2[0-9][0-9]$ ]]; then
    break
  fi

  elapsed="$(( $(date +%s) - start_ts ))"
  if (( elapsed >= health_timeout_sec )); then
    echo "[step7-regression] backend health timeout (${elapsed}s, last_code=${code:-N/A})"
    exit 1
  fi
  sleep "$health_interval_sec"
done

echo "[step7-regression] running step7 regression script"
(
  cd "$project_abs"
  REDIS_CONTAINER=redis \
  API_SCRIPT_HEALTH_URL="$health_url" \
  bash ./scripts/api/run-api-script-tests.sh v7-sse-rank-push.sh
)

echo "[step7-regression] PASS"
