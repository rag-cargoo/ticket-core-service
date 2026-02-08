#!/usr/bin/env bash

policy_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(git -C "$policy_dir" rev-parse --show-toplevel)"
project_abs="$(cd "$policy_dir/.." && pwd)"
project_root="${project_abs#$repo_root/}"
project_name="$(basename "$project_abs")"

POLICY_ID="project:${project_name}"
POLICY_ROOTS=(
  "$project_root"
)

project_validate_knowledge_doc_quality() {
  local file_path="$1"
  [[ -f "$file_path" ]] || return 0

  local missing=()
  if ! grep -Eq "실패|한계|함정|Failure-First|Anti-Pattern|Antipattern|anti-pattern" "$file_path"; then
    missing+=("Failure-First 단락(실패/한계/함정)")
  fi
  if ! grep -Eq "Before|나쁜 예시|Bad Practice" "$file_path"; then
    missing+=("Before 단락(나쁜 예시)")
  fi
  if ! grep -Eq "After|모범 사례|Best Practice|개선" "$file_path"; then
    missing+=("After 단락(개선 결과)")
  fi
  if ! grep -Eq "Execution Log|Raw Log|실행 로그|테스트 결과|Result: PASS|Result: FAIL" "$file_path"; then
    missing+=("Execution Log 단락(실행/검증 로그)")
  fi

  local code_fence_count
  code_fence_count="$(grep -c '```' "$file_path" || true)"
  if [[ "$code_fence_count" -lt 2 ]]; then
    missing+=("코드 블록(``` ... ```)")
  fi

  if grep -Eq "^## Step 7: SSE 기반 실시간 순번 자동 푸시" "$file_path"; then
    if ! grep -Eq "v7-sse-rank-push\\.sh|RANK_UPDATE|ACTIVE|/api/v1/waiting-queue/subscribe" "$file_path"; then
      missing+=("Step 7 구현/검증 근거(v7 script, event, subscribe endpoint)")
    fi
  fi

  if [[ "${#missing[@]}" -gt 0 ]]; then
    echo "[chain-check][${POLICY_ID}] strict validation failed: knowledge doc quality check"
    echo "  - file: $file_path"
    local item
    for item in "${missing[@]}"; do
      echo "  - missing: $item"
    done
    return 1
  fi

  return 0
}

project_validate_api_spec_quality() {
  local file_path="$1"
  [[ -f "$file_path" ]] || return 0

  local required_tokens=(
    "Endpoint"
    "Description"
    "Parameters"
    "Request Example"
    "Response Summary"
    "Response Example"
  )

  local missing=()
  local token
  for token in "${required_tokens[@]}"; do
    if ! grep -Fq "$token" "$file_path"; then
      missing+=("$token")
    fi
  done

  if [[ "${#missing[@]}" -gt 0 ]]; then
    echo "[chain-check][${POLICY_ID}] strict validation failed: API spec 6-step check"
    echo "  - file: $file_path"
    for token in "${missing[@]}"; do
      echo "  - missing: $token"
    done
    return 1
  fi

  return 0
}

is_runtime_api_script_change() {
  local file_path="$1"
  case "$file_path" in
    "${project_root}/scripts/api/v"*.sh|\
    "${project_root}/scripts/api/common.sh"|\
    "${project_root}/scripts/api/setup-test-data.sh")
      return 0
      ;;
  esac
  return 1
}

policy_validate() {
  local mode="$1"
  local staged_files="$2"

  local sidebar_file="sidebar-manifest.md"

  local generated_artifacts=()
  local file_path
  while IFS= read -r file_path; do
    [[ -z "$file_path" ]] && continue
    if [[ "$file_path" == "${project_root}/build/"* ]] || [[ "$file_path" == "${project_root}/.gradle/"* ]]; then
      generated_artifacts+=("$file_path")
    fi
  done <<< "$staged_files"

  if [[ "${#generated_artifacts[@]}" -gt 0 ]]; then
    if [[ "$mode" == "strict" ]]; then
      echo "[chain-check][${POLICY_ID}] strict validation failed: generated artifacts are staged"
      echo "[chain-check][${POLICY_ID}] unstage build outputs before commit:"
      local artifact
      for artifact in "${generated_artifacts[@]}"; do
        echo "  - $artifact"
      done
      return 1
    fi
    echo "[chain-check][${POLICY_ID}] quick mode warning: generated artifacts are staged"
  fi

  local staged_knowledge_docs=()
  local staged_api_specs=()
  while IFS= read -r file_path; do
    [[ -z "$file_path" ]] && continue
    if [[ "$file_path" == "${project_root}/prj-docs/knowledge/"*.md ]]; then
      staged_knowledge_docs+=("$file_path")
      continue
    fi
    if [[ "$file_path" == "${project_root}/prj-docs/api-specs/"*.md ]]; then
      staged_api_specs+=("$file_path")
      continue
    fi
  done <<< "$staged_files"

  if [[ "$mode" == "strict" ]]; then
    for file_path in "${staged_knowledge_docs[@]}"; do
      project_validate_knowledge_doc_quality "$file_path" || return 1
    done
    for file_path in "${staged_api_specs[@]}"; do
      project_validate_api_spec_quality "$file_path" || return 1
    done
  fi

  local required_baseline_files=(
    "${project_root}/README.md"
    "${project_root}/prj-docs/PROJECT_AGENT.md"
    "${project_root}/prj-docs/task.md"
    "${project_root}/prj-docs/meeting-notes/README.md"
  )
  local required_baseline_dirs=(
    "${project_root}/prj-docs/rules"
  )
  local baseline_missing=()
  local baseline_item
  for baseline_item in "${required_baseline_files[@]}"; do
    if [[ ! -f "$baseline_item" ]]; then
      baseline_missing+=("$baseline_item")
    fi
  done
  for baseline_item in "${required_baseline_dirs[@]}"; do
    if [[ ! -d "$baseline_item" ]]; then
      baseline_missing+=("${baseline_item}/")
    fi
  done

  if [[ "${#baseline_missing[@]}" -gt 0 ]]; then
    if [[ "$mode" == "strict" ]]; then
      echo "[chain-check][${POLICY_ID}] strict validation failed: project baseline is incomplete"
      for baseline_item in "${baseline_missing[@]}"; do
        echo "  - missing: $baseline_item"
      done
      return 1
    fi
    echo "[chain-check][${POLICY_ID}] quick mode warning: project baseline is incomplete"
    for baseline_item in "${baseline_missing[@]}"; do
      echo "  - missing: $baseline_item"
    done
  fi

  local project_change_patterns=(
    "^${project_root}/src/main/java/.+\\.java$"
    "^${project_root}/src/main/resources/.+\\.ya?ml$"
    "^${project_root}/scripts/api/(v[0-9].*\\.sh|common\\.sh|setup-test-data\\.sh)$"
  )

  local needs_chain="false"
  local pattern
  for pattern in "${project_change_patterns[@]}"; do
    if grep -Eq "$pattern" <<< "$staged_files"; then
      needs_chain="true"
      break
    fi
  done

  if [[ "$needs_chain" != "true" ]]; then
    if [[ "$mode" == "strict" ]]; then
      echo "[chain-check][${POLICY_ID}] strict mode: chain trigger not detected, skip"
    fi
    return 0
  fi

  local added_docs
  added_docs="$(git -c core.quotePath=false diff --cached --name-only --diff-filter=A | grep -E "^${project_root}/prj-docs/.+\\.md$" || true)"
  local sidebar_missing="false"
  if [[ -n "$added_docs" ]] && ! grep -Fxq "$sidebar_file" <<< "$staged_files"; then
    sidebar_missing="true"
  fi

  local has_api_script_change="false"
  while IFS= read -r file_path; do
    [[ -z "$file_path" ]] && continue
    if is_runtime_api_script_change "$file_path"; then
      has_api_script_change="true"
      break
    fi
  done <<< "$staged_files"

  while IFS= read -r file_path; do
    [[ -z "$file_path" ]] && continue
    if [[ "$file_path" == "${project_root}/scripts/api/"*.sh ]]; then
      bash -n "$file_path"
    fi
  done <<< "$staged_files"

  if [[ "$has_api_script_change" == "true" && "$mode" != "strict" ]]; then
    echo "[chain-check][${POLICY_ID}] quick mode: API script execution test skipped"
    echo "[chain-check][${POLICY_ID}] hint: run strict before milestone commit"
    echo "  - CHAIN_VALIDATION_MODE=strict git commit ..."
  fi

  if [[ "$mode" != "strict" ]]; then
    local quick_hints=()
    if ! grep -Fxq "${project_root}/prj-docs/task.md" <<< "$staged_files"; then
      quick_hints+=("${project_root}/prj-docs/task.md")
    fi
    if ! grep -Eq "^${project_root}/prj-docs/api-specs/.+\\.md$" <<< "$staged_files"; then
      quick_hints+=("${project_root}/prj-docs/api-specs/*.md")
    fi
    if ! grep -Eq "^${project_root}/prj-docs/knowledge/.+\\.md$" <<< "$staged_files"; then
      quick_hints+=("${project_root}/prj-docs/knowledge/*.md")
    fi
    if ! grep -Eq "^${project_root}/scripts/http/.+\\.http$" <<< "$staged_files"; then
      quick_hints+=("${project_root}/scripts/http/*.http")
    fi
    if ! grep -Eq "^${project_root}/scripts/api/.+\\.sh$" <<< "$staged_files"; then
      quick_hints+=("${project_root}/scripts/api/*.sh")
    fi

    if [[ "${#quick_hints[@]}" -gt 0 ]]; then
      echo "[chain-check][${POLICY_ID}] quick mode hint: strict mode will require:"
      local hint
      for hint in "${quick_hints[@]}"; do
        echo "  - $hint"
      done
    fi
    if [[ "$sidebar_missing" == "true" ]]; then
      echo "[chain-check][${POLICY_ID}] quick mode hint: update sidebar before strict commit"
      echo "  - ${sidebar_file}"
    fi

    echo "[chain-check][${POLICY_ID}] validation ok (quick)"
    return 0
  fi

  local missing=()
  if ! grep -Fxq "${project_root}/prj-docs/task.md" <<< "$staged_files"; then
    missing+=("${project_root}/prj-docs/task.md")
  fi
  if ! grep -Eq "^${project_root}/prj-docs/api-specs/.+\\.md$" <<< "$staged_files"; then
    missing+=("${project_root}/prj-docs/api-specs/*.md (at least one)")
  fi
  if ! grep -Eq "^${project_root}/prj-docs/knowledge/.+\\.md$" <<< "$staged_files"; then
    missing+=("${project_root}/prj-docs/knowledge/*.md (at least one)")
  fi
  if ! grep -Eq "^${project_root}/scripts/http/.+\\.http$" <<< "$staged_files"; then
    missing+=("${project_root}/scripts/http/*.http (at least one)")
  fi
  if ! grep -Eq "^${project_root}/scripts/api/.+\\.sh$" <<< "$staged_files"; then
    missing+=("${project_root}/scripts/api/*.sh (at least one)")
  fi

  if [[ "${#missing[@]}" -gt 0 ]]; then
    echo "[chain-check][${POLICY_ID}] strict validation failed"
    echo "[chain-check][${POLICY_ID}] 프로젝트 코드/설정/스크립트 변경이 감지됨"
    echo "[chain-check][${POLICY_ID}] 아래 파일군을 함께 stage 해야 커밋 가능:"
    local miss
    for miss in "${missing[@]}"; do
      echo "  - $miss"
    done
    echo
    echo "[chain-check][${POLICY_ID}] staged files:"
    echo "$staged_files" | sed 's/^/  - /'
    return 1
  fi

  if [[ "$sidebar_missing" == "true" ]]; then
    echo "[chain-check][${POLICY_ID}] new project docs detected but sidebar is not updated (strict)"
    echo "[chain-check][${POLICY_ID}] stage this file and commit again:"
    echo "  - ${sidebar_file}"
    echo
    echo "[chain-check][${POLICY_ID}] new docs:"
    echo "$added_docs" | sed 's/^/  - /'
    return 1
  fi

  if [[ "$has_api_script_change" == "true" ]]; then
    echo "[chain-check][${POLICY_ID}] running API script tests"
    bash "${project_root}/scripts/api/run-api-script-tests.sh"

    local report_file="${project_root}/prj-docs/api-test/latest.md"
    if ! grep -Fxq "$report_file" <<< "$staged_files"; then
      echo "[chain-check][${POLICY_ID}] API script test report is not staged"
      echo "[chain-check][${POLICY_ID}] stage this file and commit again:"
      echo "  - $report_file"
      return 1
    fi
    if ! git diff --quiet -- "$report_file"; then
      echo "[chain-check][${POLICY_ID}] API script test report was updated by test run"
      echo "[chain-check][${POLICY_ID}] re-stage this file and commit again:"
      echo "  - $report_file"
      return 1
    fi
  fi

  echo "[chain-check][${POLICY_ID}] validation ok (strict)"
}
