#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel)"
if [[ "$project_abs" == "$repo_root" ]]; then
  project_root="."
else
  project_root="${project_abs#$repo_root/}"
fi

report_path="${project_root}/.codex/tmp/ticket-core-service/api-test/auth-social-e2e-latest.md"
report_abs_path="${project_abs}/.codex/tmp/ticket-core-service/api-test/auth-social-e2e-latest.md"
tmp_log="$(mktemp)"
trap 'rm -f "$tmp_log"' EXIT

declare -a tests=(
  "com.ticketrush.api.controller.SocialAuthControllerIntegrationTest"
  "com.ticketrush.domain.auth.service.SocialAuthServiceTest"
  "com.ticketrush.api.controller.AuthSecurityIntegrationTest"
  "com.ticketrush.domain.auth.service.AuthSessionServiceTest"
)

cmd=("./gradlew" "test")
for test_name in "${tests[@]}"; do
  cmd+=("--tests" "$test_name")
done

mkdir -p "$(dirname "$report_abs_path")"

(
  cd "$project_abs"
  "${cmd[@]}"
) >"$tmp_log" 2>&1 || {
  cat >"$report_abs_path" <<EOF
# Auth Social E2E Pipeline Report

- Result: FAIL
- Command: \`${cmd[*]}\`
- Project Root: \`${project_root}\`

## Tail Logs
\`\`\`text
$(tail -n 80 "$tmp_log")
\`\`\`
EOF
  echo "[auth-social-e2e] failed"
  echo "[auth-social-e2e] report: ${report_path}"
  exit 1
}

cat >"$report_abs_path" <<EOF
# Auth Social E2E Pipeline Report

- Result: PASS
- Command: \`${cmd[*]}\`
- Project Root: \`${project_root}\`
- Tests:
$(for test_name in "${tests[@]}"; do echo "  - \`${test_name}\`"; done)
EOF

echo "[auth-social-e2e] passed"
echo "[auth-social-e2e] report: ${report_path}"
