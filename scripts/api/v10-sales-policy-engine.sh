#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
RESERVATION_API="${RESERVATION_API:-${API_HOST}/api/reservations/v6}"
CONCERT_API="${CONCERT_API:-${API_HOST}/api/concerts}"
USER_API="${USER_API:-${API_HOST}/api/users}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

OPEN_PRESALE_START_AT="2000-01-01T00:00:00"
OPEN_PRESALE_END_AT="2000-01-02T00:00:00"
OPEN_GENERAL_SALE_START_AT="2000-01-03T00:00:00"

PRESALE_START_AT="2000-01-01T00:00:00"
PRESALE_END_AT="2099-01-01T00:00:00"
GENERAL_SALE_START_AT="2099-01-01T00:00:00"

cleanup_users() {
  if [[ -n "${BASIC_USER_ID:-}" ]]; then
    curl -s -X DELETE "${USER_API}/${BASIC_USER_ID}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${VIP_USER_ID:-}" ]]; then
    curl -s -X DELETE "${USER_API}/${VIP_USER_ID}" >/dev/null 2>&1 || true
  fi
}

restore_open_policy() {
  if [[ -z "${CONCERT_ID:-}" ]]; then
    return 0
  fi

  curl -s -X PUT "${CONCERT_API}/${CONCERT_ID}/sales-policy" \
    -H "Content-Type: application/json" \
    -d "{
      \"presaleStartAt\":\"${OPEN_PRESALE_START_AT}\",
      \"presaleEndAt\":\"${OPEN_PRESALE_END_AT}\",
      \"presaleMinimumTier\":\"BASIC\",
      \"generalSaleStartAt\":\"${OPEN_GENERAL_SALE_START_AT}\",
      \"maxReservationsPerUser\":10
    }" >/dev/null 2>&1 || true
}

cleanup() {
  cleanup_users
  restore_open_policy
}
trap cleanup EXIT

echo -e "${BLUE}>>>> [v10 Test] Step 11 판매 정책 엔진(선예매/등급/1인 제한) 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 0] 테스트 유저 생성(BASIC/VIP)... ${NC}"
BASIC_USER_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" \
  -d "{\"username\":\"test_v10_basic_$(date +%s)\",\"tier\":\"BASIC\"}" | grep -oP '"id":\s*\K\d+' || true)
VIP_USER_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" \
  -d "{\"username\":\"test_v10_vip_$(date +%s)\",\"tier\":\"VIP\"}" | grep -oP '"id":\s*\K\d+' || true)
if [[ -z "${BASIC_USER_ID}" || -z "${VIP_USER_ID}" ]]; then
  echo -e "${RED}실패${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (basic=${BASIC_USER_ID}, vip=${VIP_USER_ID})${NC}"

echo -ne "${YELLOW}[Step 1] 대상 콘서트/옵션/좌석 식별... ${NC}"
CONCERT_ID=$(curl -s "${CONCERT_API}" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
if [[ -z "${CONCERT_ID}" ]]; then
  ensure_test_data || true
  CONCERT_ID=$(curl -s "${CONCERT_API}" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
fi
if [[ -z "${CONCERT_ID}" ]]; then
  echo -e "${RED}실패 (concert 없음)${NC}"
  exit 1
fi

OPTION_ID=$(resolve_latest_option_id "${CONCERT_API}" || true)
if [[ -z "${OPTION_ID}" ]]; then
  ensure_test_data || true
  OPTION_ID=$(resolve_latest_option_id "${CONCERT_API}" || true)
fi
if [[ -z "${OPTION_ID}" ]]; then
  echo -e "${RED}실패 (option 없음)${NC}"
  exit 1
fi

SEAT_IDS=$(curl -s "${CONCERT_API}/options/${OPTION_ID}/seats" | grep -oP '"id":\s*\K\d+' | head -n 2 | tr '\n' ' ')
read -r FIRST_SEAT_ID SECOND_SEAT_ID <<< "${SEAT_IDS}"
if [[ -z "${FIRST_SEAT_ID:-}" || -z "${SECOND_SEAT_ID:-}" ]]; then
  echo -e "${RED}실패 (available seat 2개 미만)${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (concertId=${CONCERT_ID}, optionId=${OPTION_ID}, seats=${FIRST_SEAT_ID},${SECOND_SEAT_ID})${NC}"

echo -ne "${YELLOW}[Step 2] Step11 판매 정책 설정(Presale+Tier+1인 제한)... ${NC}"

POLICY_RESPONSE=$(curl ${CURL_OPTS} -X PUT "${CONCERT_API}/${CONCERT_ID}/sales-policy" \
  -H "Content-Type: application/json" \
  -d "{
    \"presaleStartAt\":\"${PRESALE_START_AT}\",
    \"presaleEndAt\":\"${PRESALE_END_AT}\",
    \"presaleMinimumTier\":\"VIP\",
    \"generalSaleStartAt\":\"${GENERAL_SALE_START_AT}\",
    \"maxReservationsPerUser\":1
  }")
POLICY_BODY=$(echo "${POLICY_RESPONSE}" | sed '$d')
POLICY_CODE=$(echo "${POLICY_RESPONSE}" | tail -n1)
POLICY_TIER=$(echo "${POLICY_BODY}" | grep -oP '"presaleMinimumTier":"\K[^"]+' || true)
POLICY_LIMIT=$(echo "${POLICY_BODY}" | grep -oP '"maxReservationsPerUser":\K\d+' || true)
if [[ "${POLICY_CODE}" != "200" || "${POLICY_TIER}" != "VIP" || "${POLICY_LIMIT}" != "1" ]]; then
  echo -e "${RED}실패 (code=${POLICY_CODE}, body=${POLICY_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (tier=${POLICY_TIER}, limit=${POLICY_LIMIT})${NC}"

echo -ne "${YELLOW}[Step 3] BASIC 유저 선예매 차단 검증(기대: 409)... ${NC}"
BASIC_HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/holds" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${BASIC_USER_ID},\"seatId\":${FIRST_SEAT_ID}}")
BASIC_HOLD_BODY=$(echo "${BASIC_HOLD_RESPONSE}" | sed '$d')
BASIC_HOLD_CODE=$(echo "${BASIC_HOLD_RESPONSE}" | tail -n1)
if [[ "${BASIC_HOLD_CODE}" != "409" || "${BASIC_HOLD_BODY}" != *"Presale tier not eligible"* ]]; then
  echo -e "${RED}실패 (code=${BASIC_HOLD_CODE}, body=${BASIC_HOLD_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (code=${BASIC_HOLD_CODE})${NC}"

echo -ne "${YELLOW}[Step 4] VIP 유저 선예매 허용 검증(기대: HOLD)... ${NC}"
VIP_HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/holds" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${VIP_USER_ID},\"seatId\":${FIRST_SEAT_ID}}")
VIP_HOLD_BODY=$(echo "${VIP_HOLD_RESPONSE}" | sed '$d')
VIP_HOLD_CODE=$(echo "${VIP_HOLD_RESPONSE}" | tail -n1)
VIP_HOLD_STATUS=$(echo "${VIP_HOLD_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${VIP_HOLD_CODE}" != "201" || "${VIP_HOLD_STATUS}" != "HOLD" ]]; then
  echo -e "${RED}실패 (code=${VIP_HOLD_CODE}, body=${VIP_HOLD_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${VIP_HOLD_STATUS})${NC}"

echo -ne "${YELLOW}[Step 5] VIP 유저 1인 제한 검증(두 번째 HOLD 차단)... ${NC}"
VIP_SECOND_HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/holds" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${VIP_USER_ID},\"seatId\":${SECOND_SEAT_ID}}")
VIP_SECOND_HOLD_BODY=$(echo "${VIP_SECOND_HOLD_RESPONSE}" | sed '$d')
VIP_SECOND_HOLD_CODE=$(echo "${VIP_SECOND_HOLD_RESPONSE}" | tail -n1)
if [[ "${VIP_SECOND_HOLD_CODE}" != "409" || "${VIP_SECOND_HOLD_BODY}" != *"Per-user reservation limit exceeded"* ]]; then
  echo -e "${RED}실패 (code=${VIP_SECOND_HOLD_CODE}, body=${VIP_SECOND_HOLD_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (code=${VIP_SECOND_HOLD_CODE})${NC}"

echo -ne "${YELLOW}[Step 6] 정책 조회 API 검증... ${NC}"
GET_POLICY_RESPONSE=$(curl ${CURL_OPTS} "${CONCERT_API}/${CONCERT_ID}/sales-policy")
GET_POLICY_BODY=$(echo "${GET_POLICY_RESPONSE}" | sed '$d')
GET_POLICY_CODE=$(echo "${GET_POLICY_RESPONSE}" | tail -n1)
GET_POLICY_LIMIT=$(echo "${GET_POLICY_BODY}" | grep -oP '"maxReservationsPerUser":\K\d+' || true)
if [[ "${GET_POLICY_CODE}" != "200" || "${GET_POLICY_LIMIT}" != "1" ]]; then
  echo -e "${RED}실패 (code=${GET_POLICY_CODE}, body=${GET_POLICY_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (limit=${GET_POLICY_LIMIT})${NC}"

echo -e "${GREEN}>>>> [v10 Test] 검증 종료 (PASS).${NC}"
