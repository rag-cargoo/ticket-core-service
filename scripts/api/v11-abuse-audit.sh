#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
RESERVATION_API="${RESERVATION_API:-${API_HOST}/api/reservations/v6}"
USER_API="${USER_API:-${API_HOST}/api/users}"
CONCERT_API="${CONCERT_API:-${API_HOST}/api/concerts}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

RUN_ID="$(date +%s%N)"

cleanup_users() {
  for uid in "${USER_A_ID:-}" "${USER_B_ID:-}" "${USER_C_ID:-}" "${USER_D_ID:-}"; do
    [[ -n "${uid}" ]] || continue
    curl -s -X DELETE "${USER_API}/${uid}" >/dev/null 2>&1 || true
  done
}
trap cleanup_users EXIT

hold_call() {
  local user_id="$1"
  local seat_id="$2"
  local request_fp="$3"
  local device_fp="$4"
  curl ${CURL_OPTS} -X POST "${RESERVATION_API}/holds" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":${user_id},\"seatId\":${seat_id},\"requestFingerprint\":\"${request_fp}\",\"deviceFingerprint\":\"${device_fp}\"}"
}

echo -e "${BLUE}>>>> [v11 Test] Step 12 부정사용 방지/감사 추적 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 0] 테스트 유저 4명 생성... ${NC}"
USER_A_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" -d "{\"username\":\"test_v11_user_a_$(date +%s)\",\"tier\":\"BASIC\"}" | grep -oP '"id":\s*\K\d+' || true)
USER_B_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" -d "{\"username\":\"test_v11_user_b_$(date +%s)\",\"tier\":\"BASIC\"}" | grep -oP '"id":\s*\K\d+' || true)
USER_C_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" -d "{\"username\":\"test_v11_user_c_$(date +%s)\",\"tier\":\"BASIC\"}" | grep -oP '"id":\s*\K\d+' || true)
USER_D_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" -d "{\"username\":\"test_v11_user_d_$(date +%s)\",\"tier\":\"BASIC\"}" | grep -oP '"id":\s*\K\d+' || true)
if [[ -z "${USER_A_ID}" || -z "${USER_B_ID}" || -z "${USER_C_ID}" || -z "${USER_D_ID}" ]]; then
  echo -e "${RED}실패${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (A=${USER_A_ID}, B=${USER_B_ID}, C=${USER_C_ID}, D=${USER_D_ID})${NC}"

echo -ne "${YELLOW}[Step 1] 좌석 8개 확보... ${NC}"
OPTION_ID=$(resolve_latest_option_id "${CONCERT_API}" || true)
if [[ -z "${OPTION_ID}" ]]; then
  ensure_test_data || true
  OPTION_ID=$(resolve_latest_option_id "${CONCERT_API}" || true)
fi
if [[ -z "${OPTION_ID}" ]]; then
  echo -e "${RED}실패 (option 없음)${NC}"
  exit 1
fi
collect_seat_ids() {
  local option_id="$1"
  curl -s "${CONCERT_API}/options/${option_id}/seats" | grep -oP '"id":\s*\K\d+' | head -n 8 | tr '\n' ' '
}

SEAT_IDS=$(collect_seat_ids "${OPTION_ID}")
read -r S1 S2 S3 S4 S5 S6 S7 S8 <<< "${SEAT_IDS}"
if [[ -z "${S8:-}" ]]; then
  ensure_test_data || true
  OPTION_ID=$(resolve_latest_option_id "${CONCERT_API}" || true)
  if [[ -n "${OPTION_ID}" ]]; then
    SEAT_IDS=$(collect_seat_ids "${OPTION_ID}")
    read -r S1 S2 S3 S4 S5 S6 S7 S8 <<< "${SEAT_IDS}"
  fi
fi
if [[ -z "${S8:-}" ]]; then
  echo -e "${RED}실패 (seat 8개 미만)${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (optionId=${OPTION_ID})${NC}"

echo -ne "${YELLOW}[Step 1.5] 일반 판매 가능 정책 보정... ${NC}"
CONCERT_ID=$(curl -s "${CONCERT_API}" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
if [[ -z "${CONCERT_ID}" ]]; then
  echo -e "${RED}실패 (concert 없음)${NC}"
  exit 1
fi
POLICY_RESPONSE=$(curl ${CURL_OPTS} -X PUT "${CONCERT_API}/${CONCERT_ID}/sales-policy" \
  -H "Content-Type: application/json" \
  -d "{
    \"presaleStartAt\":\"2000-01-01T00:00:00\",
    \"presaleEndAt\":\"2000-01-02T00:00:00\",
    \"presaleMinimumTier\":\"BASIC\",
    \"generalSaleStartAt\":\"2000-01-03T00:00:00\",
    \"maxReservationsPerUser\":10
  }")
POLICY_BODY=$(echo "${POLICY_RESPONSE}" | sed '$d')
POLICY_CODE=$(echo "${POLICY_RESPONSE}" | tail -n1)
if [[ "${POLICY_CODE}" != "200" ]]; then
  echo -e "${RED}실패 (code=${POLICY_CODE}, body=${POLICY_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

RATE_DEVICE_FP="device-a-${RUN_ID}"
DUP_DEVICE_FP="device-b-${RUN_ID}"
SHARED_DEVICE_FP="shared-device-${RUN_ID}"
DUP_REQUEST_FP="dup-b-${RUN_ID}"

echo -ne "${YELLOW}[Step 2] 유저 A rate-limit 검증 (4번째 차단 기대)... ${NC}"
R1=$(hold_call "${USER_A_ID}" "${S1}" "rate-a-1-${RUN_ID}" "${RATE_DEVICE_FP}")
R1_BODY=$(echo "${R1}" | sed '$d'); R1_CODE=$(echo "${R1}" | tail -n1)
R2=$(hold_call "${USER_A_ID}" "${S2}" "rate-a-2-${RUN_ID}" "${RATE_DEVICE_FP}")
R2_BODY=$(echo "${R2}" | sed '$d'); R2_CODE=$(echo "${R2}" | tail -n1)
R3=$(hold_call "${USER_A_ID}" "${S3}" "rate-a-3-${RUN_ID}" "${RATE_DEVICE_FP}")
R3_BODY=$(echo "${R3}" | sed '$d'); R3_CODE=$(echo "${R3}" | tail -n1)
R4=$(hold_call "${USER_A_ID}" "${S4}" "rate-a-4-${RUN_ID}" "${RATE_DEVICE_FP}")
R4_BODY=$(echo "${R4}" | sed '$d'); R4_CODE=$(echo "${R4}" | tail -n1)
if [[ "${R1_CODE}" != "201" || "${R2_CODE}" != "201" || "${R3_CODE}" != "201" || "${R4_CODE}" != "409" || "${R4_BODY}" != *"Rate limit exceeded"* ]]; then
  echo -e "${RED}실패 (codes=${R1_CODE}/${R2_CODE}/${R3_CODE}/${R4_CODE}, body=${R4_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (4th blocked: ${R4_CODE})${NC}"

echo -ne "${YELLOW}[Step 3] 유저 B duplicate fingerprint 검증... ${NC}"
D1=$(hold_call "${USER_B_ID}" "${S5}" "${DUP_REQUEST_FP}" "${DUP_DEVICE_FP}")
D1_BODY=$(echo "${D1}" | sed '$d'); D1_CODE=$(echo "${D1}" | tail -n1)
D2=$(hold_call "${USER_B_ID}" "${S6}" "${DUP_REQUEST_FP}" "${DUP_DEVICE_FP}")
D2_BODY=$(echo "${D2}" | sed '$d'); D2_CODE=$(echo "${D2}" | tail -n1)
if [[ "${D1_CODE}" != "201" || "${D2_CODE}" != "409" || "${D2_BODY}" != *"Duplicate request fingerprint detected"* ]]; then
  echo -e "${RED}실패 (codes=${D1_CODE}/${D2_CODE}, body=${D2_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (duplicate blocked: ${D2_CODE})${NC}"

echo -ne "${YELLOW}[Step 4] shared device multi-account 검증... ${NC}"
M1=$(hold_call "${USER_C_ID}" "${S7}" "multi-c-1-${RUN_ID}" "${SHARED_DEVICE_FP}")
M1_BODY=$(echo "${M1}" | sed '$d'); M1_CODE=$(echo "${M1}" | tail -n1)
M2=$(hold_call "${USER_D_ID}" "${S8}" "multi-d-1-${RUN_ID}" "${SHARED_DEVICE_FP}")
M2_BODY=$(echo "${M2}" | sed '$d'); M2_CODE=$(echo "${M2}" | tail -n1)
if [[ "${M1_CODE}" != "201" || "${M2_CODE}" != "409" || "${M2_BODY}" != *"Device fingerprint used by multiple accounts"* ]]; then
  echo -e "${RED}실패 (codes=${M1_CODE}/${M2_CODE}, body=${M2_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (multi-account blocked: ${M2_CODE})${NC}"

echo -ne "${YELLOW}[Step 5] 감사 조회 API 검증... ${NC}"
A_AUDIT=$(curl ${CURL_OPTS} "${RESERVATION_API}/audit/abuse?userId=${USER_A_ID}&result=BLOCKED&limit=20")
A_AUDIT_BODY=$(echo "${A_AUDIT}" | sed '$d'); A_AUDIT_CODE=$(echo "${A_AUDIT}" | tail -n1)
B_AUDIT=$(curl ${CURL_OPTS} "${RESERVATION_API}/audit/abuse?userId=${USER_B_ID}&result=BLOCKED&limit=20")
B_AUDIT_BODY=$(echo "${B_AUDIT}" | sed '$d'); B_AUDIT_CODE=$(echo "${B_AUDIT}" | tail -n1)
D_AUDIT=$(curl ${CURL_OPTS} "${RESERVATION_API}/audit/abuse?userId=${USER_D_ID}&result=BLOCKED&limit=20")
D_AUDIT_BODY=$(echo "${D_AUDIT}" | sed '$d'); D_AUDIT_CODE=$(echo "${D_AUDIT}" | tail -n1)
if [[ "${A_AUDIT_CODE}" != "200" || "${B_AUDIT_CODE}" != "200" || "${D_AUDIT_CODE}" != "200" \
   || "${A_AUDIT_BODY}" != *"RATE_LIMIT_EXCEEDED"* \
   || "${B_AUDIT_BODY}" != *"DUPLICATE_REQUEST_FINGERPRINT"* \
   || "${D_AUDIT_BODY}" != *"DEVICE_FINGERPRINT_MULTI_ACCOUNT"* ]]; then
  echo -e "${RED}실패 (audit code/body 확인 필요)${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (차단 사유 3종 조회됨)${NC}"

echo -e "${GREEN}>>>> [v11 Test] 검증 종료 (PASS).${NC}"
