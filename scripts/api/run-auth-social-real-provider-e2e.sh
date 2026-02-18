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

report_path="${project_root}/.codex/tmp/ticket-core-service/api-test/auth-social-real-provider-e2e-latest.md"
report_abs_path="${project_abs}/.codex/tmp/ticket-core-service/api-test/auth-social-real-provider-e2e-latest.md"
tmp_log="$(mktemp)"
trap 'rm -f "$tmp_log"' EXIT

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
PROVIDER="${AUTH_REAL_E2E_PROVIDER:-kakao}"
AUTH_CODE="${AUTH_REAL_E2E_CODE:-}"
AUTH_STATE="${AUTH_REAL_E2E_STATE:-}"
PREPARE_ONLY="${AUTH_REAL_E2E_PREPARE_ONLY:-false}"
REAL_E2E_ENABLED="${APP_AUTH_SOCIAL_REAL_E2E_ENABLED:-false}"

mkdir -p "$(dirname "$report_abs_path")"

log() {
  echo "$1" | tee -a "$tmp_log" >/dev/null
}

write_report() {
  local result="$1"
  local note="$2"

  cat >"$report_abs_path" <<EOF
# Auth Social Real Provider E2E Report

- Result: ${result}
- API Host: \`${API_HOST}\`
- Provider: \`${PROVIDER}\`
- Real E2E Enabled: \`${REAL_E2E_ENABLED}\`
- Prepare Only: \`${PREPARE_ONLY}\`
- Note: ${note}

## Logs
\`\`\`text
$(cat "$tmp_log")
\`\`\`
EOF
}

fail() {
  local message="$1"
  log "[FAIL] ${message}"
  write_report "FAIL" "${message}"
  echo "[auth-social-real-provider-e2e] failed"
  echo "[auth-social-real-provider-e2e] report: ${report_path}"
  exit 1
}

require_json_field() {
  local json_payload="$1"
  local key="$2"
  python3 - "$json_payload" "$key" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
key = sys.argv[2]
value = payload.get(key)
if value is None:
    raise SystemExit(1)
print(value)
PY
}

extract_authorize_info() {
  local url="$1"
  python3 - "$url" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
print(payload.get("authorizeUrl", ""))
print(payload.get("state", ""))
PY
}

build_exchange_payload() {
  python3 - "$PROVIDER" "$AUTH_CODE" "$AUTH_STATE" <<'PY'
import json
import sys

provider = sys.argv[1].lower().strip()
code = sys.argv[2]
state = sys.argv[3]

payload = {"code": code}
if provider == "naver":
    payload["state"] = state
elif state:
    payload["state"] = state

print(json.dumps(payload))
PY
}

if [[ "$PROVIDER" != "kakao" && "$PROVIDER" != "naver" ]]; then
  fail "AUTH_REAL_E2E_PROVIDER must be one of kakao|naver (actual: ${PROVIDER})"
fi

if [[ "$REAL_E2E_ENABLED" != "true" ]]; then
  fail "Set APP_AUTH_SOCIAL_REAL_E2E_ENABLED=true before running real-provider e2e."
fi

if [[ "$PROVIDER" == "naver" && -z "$AUTH_STATE" ]]; then
  AUTH_STATE="real-e2e-naver-$(date +%s)"
  log "[INFO] AUTH_REAL_E2E_STATE was empty. Generated state: ${AUTH_STATE}"
fi

AUTH_URL_PATH="/api/auth/social/${PROVIDER}/authorize-url"
if [[ -n "$AUTH_STATE" ]]; then
  AUTH_URL_PATH="${AUTH_URL_PATH}?state=${AUTH_STATE}"
fi

log "[STEP] Request authorize-url: ${API_HOST}${AUTH_URL_PATH}"
AUTHORIZE_RAW="$(curl -s -w $'\n%{http_code}' "${API_HOST}${AUTH_URL_PATH}")" || fail "authorize-url request failed"
AUTHORIZE_BODY="$(echo "$AUTHORIZE_RAW" | sed '$d')"
AUTHORIZE_CODE="$(echo "$AUTHORIZE_RAW" | tail -n 1)"
if [[ "$AUTHORIZE_CODE" != "200" ]]; then
  fail "authorize-url failed (code=${AUTHORIZE_CODE}, body=${AUTHORIZE_BODY})"
fi

AUTHORIZE_URL="$(extract_authorize_info "$AUTHORIZE_BODY" | sed -n '1p')"
AUTHORIZE_STATE="$(extract_authorize_info "$AUTHORIZE_BODY" | sed -n '2p')"
if [[ -n "$AUTHORIZE_STATE" ]]; then
  AUTH_STATE="$AUTHORIZE_STATE"
fi
log "[INFO] provider authorize url ready"
log "[INFO] open this URL in browser and complete login:"
log "${AUTHORIZE_URL}"
log "[INFO] expected state: ${AUTH_STATE}"

if [[ "$PREPARE_ONLY" == "true" ]]; then
  write_report "PREPARED" "Authorize URL generated. Use callback code and rerun with AUTH_REAL_E2E_CODE."
  echo "[auth-social-real-provider-e2e] prepared"
  echo "[auth-social-real-provider-e2e] report: ${report_path}"
  exit 0
fi

if [[ -z "$AUTH_CODE" ]]; then
  fail "AUTH_REAL_E2E_CODE is required. Re-run with callback code."
fi

EXCHANGE_PAYLOAD="$(build_exchange_payload)"
log "[STEP] Exchange social code (${PROVIDER})"
EXCHANGE_RAW="$(curl -s -w $'\n%{http_code}' -X POST "${API_HOST}/api/auth/social/${PROVIDER}/exchange" \
  -H "Content-Type: application/json" \
  -d "${EXCHANGE_PAYLOAD}")" || fail "social exchange request failed"
EXCHANGE_BODY="$(echo "$EXCHANGE_RAW" | sed '$d')"
EXCHANGE_CODE="$(echo "$EXCHANGE_RAW" | tail -n 1)"
if [[ "$EXCHANGE_CODE" != "200" ]]; then
  fail "social exchange failed (code=${EXCHANGE_CODE}, body=${EXCHANGE_BODY})"
fi

ACCESS_TOKEN="$(require_json_field "$EXCHANGE_BODY" "accessToken")" || fail "accessToken missing in exchange response"
REFRESH_TOKEN="$(require_json_field "$EXCHANGE_BODY" "refreshToken")" || fail "refreshToken missing in exchange response"
USER_ID="$(require_json_field "$EXCHANGE_BODY" "userId")" || fail "userId missing in exchange response"
log "[INFO] social exchange success (userId=${USER_ID}, accessLen=${#ACCESS_TOKEN}, refreshLen=${#REFRESH_TOKEN})"

log "[STEP] Verify /api/auth/me with issued access token"
ME_RAW="$(curl -s -w $'\n%{http_code}' -X GET "${API_HOST}/api/auth/me" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")" || fail "/api/auth/me request failed"
ME_BODY="$(echo "$ME_RAW" | sed '$d')"
ME_CODE="$(echo "$ME_RAW" | tail -n 1)"
if [[ "$ME_CODE" != "200" ]]; then
  fail "/api/auth/me failed (code=${ME_CODE}, body=${ME_BODY})"
fi
log "[INFO] /api/auth/me success"

log "[STEP] Logout with issued refresh/access tokens"
LOGOUT_RAW="$(curl -s -w $'\n%{http_code}' -X POST "${API_HOST}/api/auth/logout" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"${REFRESH_TOKEN}\"}")" || fail "/api/auth/logout request failed"
LOGOUT_BODY="$(echo "$LOGOUT_RAW" | sed '$d')"
LOGOUT_CODE="$(echo "$LOGOUT_RAW" | tail -n 1)"
if [[ "$LOGOUT_CODE" != "200" ]]; then
  fail "/api/auth/logout failed (code=${LOGOUT_CODE}, body=${LOGOUT_BODY})"
fi
log "[INFO] logout success"

log "[STEP] Verify revoked access token is blocked"
REUSED_ME_RAW="$(curl -s -w $'\n%{http_code}' -X GET "${API_HOST}/api/auth/me" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")" || fail "reused /api/auth/me request failed"
REUSED_ME_BODY="$(echo "$REUSED_ME_RAW" | sed '$d')"
REUSED_ME_CODE="$(echo "$REUSED_ME_RAW" | tail -n 1)"
REUSED_ME_ERROR_CODE="$(python3 - "$REUSED_ME_BODY" <<'PY'
import json
import sys
try:
    payload = json.loads(sys.argv[1])
except Exception:
    payload = {}
print(payload.get("errorCode", ""))
PY
)"
if [[ "$REUSED_ME_CODE" != "401" || "$REUSED_ME_ERROR_CODE" != "AUTH_ACCESS_TOKEN_REVOKED" ]]; then
  fail "reused access token guard failed (code=${REUSED_ME_CODE}, errorCode=${REUSED_ME_ERROR_CODE}, body=${REUSED_ME_BODY})"
fi
log "[INFO] revoked access token guard success"

log "[STEP] Verify revoked refresh token is blocked"
REUSED_REFRESH_RAW="$(curl -s -w $'\n%{http_code}' -X POST "${API_HOST}/api/auth/token/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"${REFRESH_TOKEN}\"}")" || fail "reused refresh request failed"
REUSED_REFRESH_BODY="$(echo "$REUSED_REFRESH_RAW" | sed '$d')"
REUSED_REFRESH_CODE="$(echo "$REUSED_REFRESH_RAW" | tail -n 1)"
REUSED_REFRESH_ERROR_CODE="$(python3 - "$REUSED_REFRESH_BODY" <<'PY'
import json
import sys
try:
    payload = json.loads(sys.argv[1])
except Exception:
    payload = {}
print(payload.get("errorCode", ""))
PY
)"
if [[ "$REUSED_REFRESH_CODE" != "400" || "$REUSED_REFRESH_ERROR_CODE" != "AUTH_REFRESH_TOKEN_EXPIRED_OR_REVOKED" ]]; then
  fail "reused refresh token guard failed (code=${REUSED_REFRESH_CODE}, errorCode=${REUSED_REFRESH_ERROR_CODE}, body=${REUSED_REFRESH_BODY})"
fi
log "[INFO] revoked refresh token guard success"

write_report "PASS" "Real provider e2e completed successfully."
echo "[auth-social-real-provider-e2e] passed"
echo "[auth-social-real-provider-e2e] report: ${report_path}"
