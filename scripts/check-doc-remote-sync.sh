#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: check-doc-remote-sync.sh [--scope staged|all] [--files-file <path>]

Options:
  --scope staged|all   Target file scope. Default: staged
  --files-file <path>  Optional newline-separated file list (repo-relative).

Rule:
  - If Issue/PR links exist in task/meeting docs, remote CLOSED/MERGED state must not stay in the same block with TODO/DOING/[ ] markers.
  - If links are missing, this check emits warning only (no hard fail).
EOF
}

scope="staged"
files_file=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scope)
      if [[ $# -lt 2 ]]; then
        echo "[doc-sync] --scope requires a value" >&2
        usage
        exit 1
      fi
      scope="$2"
      shift 2
      ;;
    --files-file)
      if [[ $# -lt 2 ]]; then
        echo "[doc-sync] --files-file requires a value" >&2
        usage
        exit 1
      fi
      files_file="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[doc-sync] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

case "$scope" in
  staged|all) ;;
  *)
    echo "[doc-sync] invalid scope: $scope (allowed: staged, all)" >&2
    exit 1
    ;;
esac

if ! command -v gh >/dev/null 2>&1; then
  echo "[doc-sync] strict-remote failed: gh CLI is required" >&2
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_abs="$(cd "$script_dir/.." && pwd)"
repo_root="$(git -C "$script_dir" rev-parse --show-toplevel)"
project_root="${project_abs#$repo_root/}"
cd "$repo_root"

task_doc="${project_root}/prj-docs/task.md"
meeting_notes_dir="${project_root}/prj-docs/meeting-notes"

is_target_doc() {
  local path="$1"
  if [[ "$path" == "$task_doc" ]]; then
    return 0
  fi
  if [[ "$path" == "${meeting_notes_dir}/README.md" ]]; then
    return 1
  fi
  if [[ "$path" == "${meeting_notes_dir}/"*.md ]]; then
    return 0
  fi
  return 1
}

declare -A file_seen=()
candidate_files=()

add_candidate_file() {
  local path="$1"
  [[ -z "$path" ]] && return 0
  if ! is_target_doc "$path"; then
    return 0
  fi
  if [[ ! -f "$path" ]]; then
    return 0
  fi
  if [[ -n "${file_seen[$path]+x}" ]]; then
    return 0
  fi
  file_seen["$path"]="1"
  candidate_files+=("$path")
}

if [[ -n "$files_file" ]]; then
  if [[ ! -f "$files_file" ]]; then
    echo "[doc-sync] --files-file not found: $files_file" >&2
    exit 1
  fi
  while IFS= read -r path; do
    add_candidate_file "$path"
  done < "$files_file"
else
  if [[ "$scope" == "all" ]]; then
    add_candidate_file "$task_doc"
    while IFS= read -r path; do
      add_candidate_file "$path"
    done < <(find "$meeting_notes_dir" -maxdepth 1 -type f -name '*.md' | sort)
  else
    while IFS= read -r path; do
      add_candidate_file "$path"
    done < <(git -c core.quotePath=false diff --cached --name-only || true)
  fi
fi

if [[ "${#candidate_files[@]}" -eq 0 ]]; then
  echo "[doc-sync] no target docs in scope ($scope), skip"
  exit 0
fi

issue_url_regex='https://github\.com/[^/[:space:]]+/[^/[:space:]]+/issues/[0-9]+'
pr_url_regex='https://github\.com/[^/[:space:]]+/[^/[:space:]]+/pull/[0-9]+'
stale_regex='Status:[[:space:]]*(TODO|DOING)|상태:[[:space:]]*(TODO|DOING)|\[[[:space:]]\]'
section_heading_regex='^[[:space:]]*##[[:space:]]'
task_anchor_regex='^[[:space:]]*>[[:space:]]*-[[:space:]]*\[[[:space:]xX]\][[:space:]]+\*\*'

declare -A issue_state_cache=()
declare -A pr_state_cache=()

get_issue_state() {
  local issue_num="$1"
  if [[ -n "${issue_state_cache[$issue_num]+x}" ]]; then
    printf '%s' "${issue_state_cache[$issue_num]}"
    return 0
  fi
  local state
  state="$(gh issue view "$issue_num" --json state --jq .state 2>/dev/null || true)"
  if [[ -z "$state" ]]; then
    return 1
  fi
  issue_state_cache["$issue_num"]="$state"
  printf '%s' "$state"
}

get_pr_state() {
  local pr_num="$1"
  if [[ -n "${pr_state_cache[$pr_num]+x}" ]]; then
    printf '%s' "${pr_state_cache[$pr_num]}"
    return 0
  fi
  local state
  state="$(gh pr view "$pr_num" --json state --jq .state 2>/dev/null || true)"
  if [[ -z "$state" ]]; then
    return 1
  fi
  pr_state_cache["$pr_num"]="$state"
  printf '%s' "$state"
}

find_stale_line_same_block() {
  local file_path="$1"
  local ref_line="$2"
  local total_lines start_line end_line line_text line_no
  total_lines="$(wc -l < "$file_path")"

  start_line=1
  for ((line_no = ref_line; line_no >= 1; line_no--)); do
    line_text="$(sed -n "${line_no}p" "$file_path")"
    if [[ "$line_text" =~ $section_heading_regex ]]; then
      start_line="$line_no"
      break
    fi
    if [[ "$line_text" =~ $task_anchor_regex ]]; then
      start_line="$line_no"
      break
    fi
    if [[ "$line_text" =~ ^[[:space:]]*$ ]]; then
      start_line=$((line_no + 1))
      break
    fi
  done

  end_line="$total_lines"
  for ((line_no = ref_line + 1; line_no <= total_lines; line_no++)); do
    line_text="$(sed -n "${line_no}p" "$file_path")"
    if [[ "$line_text" =~ $section_heading_regex ]]; then
      end_line=$((line_no - 1))
      break
    fi
    if [[ "$line_text" =~ $task_anchor_regex ]]; then
      end_line=$((line_no - 1))
      break
    fi
    if [[ "$line_text" =~ ^[[:space:]]*$ ]]; then
      end_line=$((line_no - 1))
      break
    fi
  done

  local stale_line
  while IFS= read -r stale_line; do
    [[ -z "$stale_line" ]] && continue
    if (( stale_line >= start_line && stale_line <= end_line )); then
      printf '%s' "$stale_line"
      return 0
    fi
  done < <(grep -nE "$stale_regex" "$file_path" | cut -d: -f1 || true)
  return 1
}

failures=0
warnings=0

echo "[doc-sync] checking ${#candidate_files[@]} doc(s) (scope=${scope})"

for file_path in "${candidate_files[@]}"; do
  issue_refs="$(grep -nEo "$issue_url_regex" "$file_path" || true)"
  pr_refs="$(grep -nEo "$pr_url_regex" "$file_path" || true)"
  stale_lines="$(grep -nE "$stale_regex" "$file_path" || true)"

  if [[ -z "$issue_refs" && -z "$pr_refs" ]]; then
    if [[ -n "$stale_lines" ]]; then
      warnings=$((warnings + 1))
      echo "[doc-sync] warning: link missing, remote sync skipped"
      echo "  - file: $file_path"
    fi
    continue
  fi

  if [[ -n "$issue_refs" ]]; then
    while IFS= read -r ref_line; do
      [[ -z "$ref_line" ]] && continue
      ref_lineno="${ref_line%%:*}"
      ref_url="${ref_line#*:}"
      issue_num="${ref_url##*/}"
      issue_state="$(get_issue_state "$issue_num" || true)"
      if [[ -z "$issue_state" ]]; then
        echo "[doc-sync] failed: cannot read issue state"
        echo "  - issue: #$issue_num"
        echo "  - file: $file_path"
        failures=$((failures + 1))
        continue
      fi
      if [[ "$issue_state" == "CLOSED" ]]; then
        if stale_near="$(find_stale_line_same_block "$file_path" "$ref_lineno")"; then
          stale_text="$(sed -n "${stale_near}p" "$file_path" | sed 's/^[[:space:]]*//')"
          echo "[doc-sync] mismatch: CLOSED issue with in-progress marker in same section"
          echo "  - file: $file_path"
          echo "  - issue: #$issue_num (line ${ref_lineno})"
          echo "  - stale line ${stale_near}: ${stale_text}"
          failures=$((failures + 1))
        fi
      fi
    done <<< "$issue_refs"
  fi

  if [[ -n "$pr_refs" ]]; then
    while IFS= read -r ref_line; do
      [[ -z "$ref_line" ]] && continue
      ref_lineno="${ref_line%%:*}"
      ref_url="${ref_line#*:}"
      pr_num="${ref_url##*/}"
      pr_state="$(get_pr_state "$pr_num" || true)"
      if [[ -z "$pr_state" ]]; then
        echo "[doc-sync] failed: cannot read PR state"
        echo "  - pr: #$pr_num"
        echo "  - file: $file_path"
        failures=$((failures + 1))
        continue
      fi
      if [[ "$pr_state" == "MERGED" || "$pr_state" == "CLOSED" ]]; then
        if stale_near="$(find_stale_line_same_block "$file_path" "$ref_lineno")"; then
          stale_text="$(sed -n "${stale_near}p" "$file_path" | sed 's/^[[:space:]]*//')"
          echo "[doc-sync] mismatch: ${pr_state} PR with in-progress marker in same section"
          echo "  - file: $file_path"
          echo "  - pr: #$pr_num (line ${ref_lineno})"
          echo "  - stale line ${stale_near}: ${stale_text}"
          failures=$((failures + 1))
        fi
      fi
    done <<< "$pr_refs"
  fi
done

if (( failures > 0 )); then
  echo "[doc-sync] remote sync check failed (failures=${failures}, warnings=${warnings})"
  exit 1
fi

echo "[doc-sync] remote sync check ok (warnings=${warnings})"
