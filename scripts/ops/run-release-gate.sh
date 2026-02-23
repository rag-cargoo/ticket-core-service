#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"
cd "$project_abs"

tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/release-gate}"
run_stamp="${RELEASE_GATE_RUN_STAMP:-$(date -u '+%Y%m%dT%H%M%SZ')}"
run_dir="$tmp_root/$run_stamp"
report_file="${RELEASE_GATE_REPORT_FILE:-$tmp_root/latest/release-gate-latest.md}"
log_file="${RELEASE_GATE_LOG_FILE:-$run_dir/release-gate.log}"

mkdir -p "$(dirname "$report_file")" "$(dirname "$log_file")"

set +e
{
  echo "[release-gate] compile"
  ./gradlew --no-daemon compileJava compileTestJava
  echo "[release-gate] verify core tests"
  ./gradlew --no-daemon test \
    --tests 'com.ticketrush.architecture.LayerDependencyArchTest' \
    --tests 'com.ticketrush.api.controller.AuthSecurityIntegrationTest' \
    --tests 'com.ticketrush.api.controller.SocialAuthControllerIntegrationTest' \
    --tests 'com.ticketrush.application.payment.service.PaymentServiceIntegrationTest' \
    --tests 'com.ticketrush.application.reservation.service.ReservationLifecycleServiceIntegrationTest' \
    --tests 'com.ticketrush.infrastructure.messaging.KafkaPushEventConsumerTest'

  if [[ "${RELEASE_GATE_WITH_API_SCRIPTS:-false}" == "true" ]]; then
    echo "[release-gate] api script suite"
    bash ./scripts/api/run-api-script-tests.sh
  fi
} 2>&1 | tee "$log_file"
gate_rc=${PIPESTATUS[0]}
set -e

result="PASS"
if [[ "$gate_rc" -ne 0 ]]; then
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
# Release Gate Report

- Result: ${result}
- Include API Script Suite: \`${RELEASE_GATE_WITH_API_SCRIPTS:-false}\`

## Executed Gates

- compile: \`./gradlew --no-daemon compileJava compileTestJava\`
- core verify tests:
  - \`com.ticketrush.architecture.LayerDependencyArchTest\`
  - \`com.ticketrush.api.controller.AuthSecurityIntegrationTest\`
  - \`com.ticketrush.api.controller.SocialAuthControllerIntegrationTest\`
  - \`com.ticketrush.application.payment.service.PaymentServiceIntegrationTest\`
  - \`com.ticketrush.application.reservation.service.ReservationLifecycleServiceIntegrationTest\`
  - \`com.ticketrush.infrastructure.messaging.KafkaPushEventConsumerTest\`
- optional api script suite: \`scripts/api/run-api-script-tests.sh\`

## Artifacts

- release gate log: \`${log_display}\`
EOF

echo "[release-gate] report: $report_display"
exit "$gate_rc"
