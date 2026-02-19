#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel)"
if [[ "$project_abs" == "$repo_root" ]]; then
  project_root="."
  scripts_root="scripts/api"
else
  project_root="${project_abs#$repo_root/}"
  scripts_root="${project_root}/scripts/api"
fi

scripts_abs_root="${project_abs}/scripts/api"
report_path="${project_root}/.codex/tmp/ticket-core-service/api-test/latest.md"
report_abs_path="${project_abs}/.codex/tmp/ticket-core-service/api-test/latest.md"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

# Default health probe keeps script-side dependency minimal.
# OAuth callback redirect(`/login/oauth2/code/{provider}`)와 같은 브라우저 라우트 검증은 Playwright/U1 시나리오에서 다룬다.
health_url="${API_SCRIPT_HEALTH_URL:-${TICKETRUSH_HEALTH_URL:-http://127.0.0.1:8080/api/concerts}}"
push_mode="$(printf '%s' "${APP_PUSH_MODE:-${STEP7_PUSH_MODE:-websocket}}" | tr '[:upper:]' '[:lower:]')"
if [[ "$push_mode" != "sse" && "$push_mode" != "websocket" ]]; then
  push_mode="websocket"
fi

declare -a scripts_to_run=()

if [[ "$#" -gt 0 ]]; then
  for candidate in "$@"; do
    script_abs=""
    if [[ "$candidate" == /* ]]; then
      script_abs="$candidate"
    elif [[ -f "$scripts_abs_root/$candidate" ]]; then
      script_abs="$scripts_abs_root/$candidate"
    elif [[ -f "$candidate" ]]; then
      script_abs="$(cd "$(dirname "$candidate")" && pwd)/$(basename "$candidate")"
    fi

    [[ -n "$script_abs" ]] || continue
    [[ -f "$script_abs" ]] || continue

    script_rel="${script_abs#$repo_root/}"
    if [[ "$script_rel" != ${scripts_root}/* ]]; then
      continue
    fi

    script_name="$(basename "$script_abs")"
    if [[ ! "$script_name" =~ ^(v[0-9].*|[a-z][0-9].*)\.sh$ ]]; then
      continue
    fi

    scripts_to_run+=("$script_abs")
  done
else
  while IFS= read -r script_abs; do
    [[ -z "$script_abs" ]] && continue
    scripts_to_run+=("$script_abs")
  done < <(find "$scripts_abs_root" -maxdepth 1 -type f \( -name 'v*.sh' -o -name '[a-z][0-9]*.sh' \) | sort)
fi

if [[ "${#scripts_to_run[@]}" -eq 0 ]]; then
  echo "[script-test] no script selected, skipping"
  exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[script-test] curl not found"
  exit 1
fi

health_code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 3 "$health_url" || true)"
if [[ ! "$health_code" =~ ^2[0-9][0-9]$ ]]; then
  mkdir -p "$(dirname "$report_abs_path")"
  cat >"$report_abs_path" <<EOF
# API Script Test Report

- Result: FAIL
- Reason: Backend health check failed
- Health URL: \`${health_url}\`
- Health Status Code: ${health_code:-N/A}

## Troubleshooting

1. 백엔드 앱을 실행 후 재시도하세요. 예: \`cd ${project_root} && ./gradlew bootRun\`
2. 인프라(예: Redis, PostgreSQL) 컨테이너가 필요한 경우 먼저 기동하세요.
EOF
  echo "[script-test] backend health check failed: ${health_url} (status: ${health_code:-N/A})"
  exit 1
fi

mkdir -p "$(dirname "$report_abs_path")"

pass_count=0
fail_count=0
skip_count=0
table_rows=""
failure_blocks=""
skip_blocks=""

for script_abs in "${scripts_to_run[@]}"; do
  script_name="$(basename "$script_abs")"
  log_path="${tmp_root}/${script_name%.sh}.log"

  skip_reason=""
  if [[ "$script_name" == "v7-sse-rank-push.sh" && "$push_mode" != "sse" ]]; then
    skip_reason="requires APP_PUSH_MODE=sse"
  elif [[ "$script_name" == "v13-websocket-switching.sh" && "$push_mode" != "websocket" ]]; then
    skip_reason="requires APP_PUSH_MODE=websocket"
  fi

  if [[ -n "$skip_reason" ]]; then
    result="SKIP"
    rc="-"
    skip_count=$((skip_count + 1))
    skip_blocks+=$'\n'
    skip_blocks+="### ${script_name}"$'\n\n'
    skip_blocks+="- reason: ${skip_reason}"$'\n'
    skip_blocks+="- current push mode: ${push_mode}"$'\n'
    table_rows+="| \`${script_name}\` | ${result} | ${rc} |"$'\n'
    continue
  fi

  set +e
  bash "$script_abs" >"$log_path" 2>&1
  rc=$?
  set -e

  if [[ "$rc" -eq 0 ]]; then
    result="PASS"
    pass_count=$((pass_count + 1))
  else
    result="FAIL"
    fail_count=$((fail_count + 1))
    failure_blocks+=$'\n'
    failure_blocks+="### ${script_name}"$'\n\n'
    failure_blocks+='```text'$'\n'
    failure_blocks+="$(tail -n 30 "$log_path")"$'\n'
    failure_blocks+='```'$'\n'
  fi

  table_rows+="| \`${script_name}\` | ${result} | ${rc} |"$'\n'
done

overall="PASS"
if [[ "$fail_count" -gt 0 ]]; then
  overall="FAIL"
fi

cat >"$report_abs_path" <<EOF
# API Script Test Report

- Result: ${overall}
- Health URL: \`${health_url}\`
- Push Mode: \`${push_mode}\`
- Passed: ${pass_count}
- Failed: ${fail_count}
- Skipped: ${skip_count}

## Execution Matrix

| Script | Result | Exit Code |
| --- | --- | --- |
${table_rows}
EOF

if [[ "$skip_count" -gt 0 ]]; then
  cat >>"$report_abs_path" <<EOF

## Skip Notes
${skip_blocks}
EOF
fi

if [[ "$fail_count" -gt 0 ]]; then
  cat >>"$report_abs_path" <<EOF

## Troubleshooting Notes
${failure_blocks}
EOF
fi

if [[ "$fail_count" -gt 0 ]]; then
  echo "[script-test] failed (${fail_count})"
  echo "[script-test] report: ${report_path}"
  exit 1
fi

echo "[script-test] all passed (${pass_count}), skipped (${skip_count})"
echo "[script-test] report: ${report_path}"
