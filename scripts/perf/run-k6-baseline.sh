#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/../.." && pwd)"
repo_root="$(git -C "$project_abs" rev-parse --show-toplevel 2>/dev/null || echo "$project_abs")"

tmp_root="${CODEX_TMP_DIR:-$repo_root/.codex/tmp/ticket-core-service/k6}"
summary_json="${K6_SUMMARY_JSON:-$tmp_root/latest/k6-summary.json}"
baseline_summary_json="${K6_BASELINE_SUMMARY_JSON:-$tmp_root/latest/k6-baseline-summary.json}"
report_file="${K6_BASELINE_REPORT_FILE:-$tmp_root/latest/k6-baseline-latest.md}"

max_p95_ms="${K6_BASELINE_MAX_P95_MS:-1200}"
max_fail_rate="${K6_BASELINE_MAX_FAIL_RATE:-0.02}"
min_checks_rate="${K6_BASELINE_MIN_CHECKS_RATE:-0.98}"

mkdir -p "$(dirname "$report_file")"

set +e
"$script_dir/run-k6-waiting-queue.sh"
k6_rc=$?
set -e

if [[ -s "$summary_json" ]]; then
  cp "$summary_json" "$baseline_summary_json"
fi

metric_or_na() {
  local query="$1"
  if [[ ! -s "$summary_json" ]]; then
    echo "N/A"
    return 0
  fi
  jq -r "${query} // \"N/A\"" "$summary_json" 2>/dev/null || echo "N/A"
}

float_gt() {
  local left="$1"
  local right="$2"
  awk -v a="$left" -v b="$right" 'BEGIN { if (a+0 > b+0) exit 0; exit 1 }'
}

float_lt() {
  local left="$1"
  local right="$2"
  awk -v a="$left" -v b="$right" 'BEGIN { if (a+0 < b+0) exit 0; exit 1 }'
}

p95_ms="$(metric_or_na '.metrics.http_req_duration.values."p(95)" // .metrics.http_req_duration."p(95)"')"
fail_rate="$(metric_or_na '.metrics.http_req_failed.values.rate // .metrics.http_req_failed.value')"
checks_rate="$(metric_or_na '.metrics.checks.values.rate // .metrics.checks.value')"
http_reqs="$(metric_or_na '.metrics.http_reqs.values.count // .metrics.http_reqs.count')"

result="PASS"
notes=()
if [[ "$k6_rc" -ne 0 ]]; then
  result="FAIL"
  notes+=("k6 execution failed(rc=${k6_rc})")
fi

if [[ "$p95_ms" == "N/A" ]] || [[ "$fail_rate" == "N/A" ]] || [[ "$checks_rate" == "N/A" ]]; then
  result="FAIL"
  notes+=("required metric is missing in summary json")
else
  if float_gt "$p95_ms" "$max_p95_ms"; then
    result="FAIL"
    notes+=("p95 exceeded threshold (${p95_ms} > ${max_p95_ms})")
  fi
  if float_gt "$fail_rate" "$max_fail_rate"; then
    result="FAIL"
    notes+=("http failure rate exceeded threshold (${fail_rate} > ${max_fail_rate})")
  fi
  if float_lt "$checks_rate" "$min_checks_rate"; then
    result="FAIL"
    notes+=("checks rate below threshold (${checks_rate} < ${min_checks_rate})")
  fi
fi

to_repo_relative() {
  local path="$1"
  if [[ "$path" == "$repo_root/"* ]]; then
    echo "${path#$repo_root/}"
  else
    echo "$path"
  fi
}

summary_display="$(to_repo_relative "$summary_json")"
baseline_summary_display="$(to_repo_relative "$baseline_summary_json")"
report_display="$(to_repo_relative "$report_file")"

cat >"$report_file" <<EOF
# k6 Baseline Evaluation Report

- Result: ${result}
- Threshold max p95(ms): ${max_p95_ms}
- Threshold max fail rate: ${max_fail_rate}
- Threshold min checks rate: ${min_checks_rate}

## Observed Metrics

| Metric | Value |
| --- | --- |
| http_reqs.count | ${http_reqs} |
| http_req_duration.p(95) ms | ${p95_ms} |
| http_req_failed.rate | ${fail_rate} |
| checks.rate | ${checks_rate} |

## Artifacts

- summary: \`${summary_display}\`
- baseline summary copy: \`${baseline_summary_display}\`
- baseline report: \`${report_display}\`
EOF

if [[ "${#notes[@]}" -gt 0 ]]; then
  {
    echo
    echo "## Notes"
    for note in "${notes[@]}"; do
      echo "- ${note}"
    done
  } >>"$report_file"
fi

echo "[k6-baseline] report: $report_display"
if [[ "$result" == "FAIL" ]]; then
  exit 1
fi
