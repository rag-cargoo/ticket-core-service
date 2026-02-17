#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL:-${API_HOST}/api/reservations/v6}"
USER_API="${USER_API:-${API_HOST}/api/users}"
CONCERT_API="${CONCERT_API:-${API_HOST}/api/concerts}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}>>>> [v8 Test] Step 9 예약 상태머신 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 0] 테스트 유저 생성... ${NC}"
USER_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" \
  -d "{\"username\":\"test_v8_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [[ -z "${USER_ID}" ]]; then
  echo -e "${RED}실패${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (userId=${USER_ID})${NC}"

echo -ne "${YELLOW}[Step 1] 가용 좌석 조회... ${NC}"
SEAT_ID=$(find_available_seat_id "${CONCERT_API}" || true)
if [[ -z "${SEAT_ID}" ]]; then
  echo -e "${RED}실패 (available seat 없음)${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (seatId=${SEAT_ID})${NC}"

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

echo -ne "${YELLOW}[Step 2] HOLD 생성... ${NC}"
HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${BASE_URL}/holds" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${USER_ID},\"seatId\":${SEAT_ID}}")
HOLD_BODY=$(echo "${HOLD_RESPONSE}" | sed '$d')
HOLD_CODE=$(echo "${HOLD_RESPONSE}" | tail -n1)
HOLD_STATUS=$(echo "${HOLD_BODY}" | grep -oP '"status":"\K[^"]+' || true)
RESERVATION_ID=$(echo "${HOLD_BODY}" | grep -oP '"id":\s*\K\d+' || true)
if [[ "${HOLD_CODE}" != "201" || "${HOLD_STATUS}" != "HOLD" || -z "${RESERVATION_ID}" ]]; then
  echo -e "${RED}실패 (code=${HOLD_CODE}, body=${HOLD_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (reservationId=${RESERVATION_ID}, status=${HOLD_STATUS})${NC}"

echo -ne "${YELLOW}[Step 3] PAYING 전이... ${NC}"
PAYING_RESPONSE=$(curl ${CURL_OPTS} -X POST "${BASE_URL}/${RESERVATION_ID}/paying" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${USER_ID}}")
PAYING_BODY=$(echo "${PAYING_RESPONSE}" | sed '$d')
PAYING_CODE=$(echo "${PAYING_RESPONSE}" | tail -n1)
PAYING_STATUS=$(echo "${PAYING_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${PAYING_CODE}" != "200" || "${PAYING_STATUS}" != "PAYING" ]]; then
  echo -e "${RED}실패 (code=${PAYING_CODE}, body=${PAYING_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${PAYING_STATUS})${NC}"

echo -ne "${YELLOW}[Step 4] CONFIRMED 전이... ${NC}"
CONFIRM_RESPONSE=$(curl ${CURL_OPTS} -X POST "${BASE_URL}/${RESERVATION_ID}/confirm" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${USER_ID}}")
CONFIRM_BODY=$(echo "${CONFIRM_RESPONSE}" | sed '$d')
CONFIRM_CODE=$(echo "${CONFIRM_RESPONSE}" | tail -n1)
CONFIRM_STATUS=$(echo "${CONFIRM_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${CONFIRM_CODE}" != "200" || "${CONFIRM_STATUS}" != "CONFIRMED" ]]; then
  echo -e "${RED}실패 (code=${CONFIRM_CODE}, body=${CONFIRM_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${CONFIRM_STATUS})${NC}"

echo -ne "${YELLOW}[Step 5] 상태 조회... ${NC}"
GET_RESPONSE=$(curl ${CURL_OPTS} "${BASE_URL}/${RESERVATION_ID}?userId=${USER_ID}")
GET_BODY=$(echo "${GET_RESPONSE}" | sed '$d')
GET_CODE=$(echo "${GET_RESPONSE}" | tail -n1)
GET_STATUS=$(echo "${GET_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${GET_CODE}" != "200" || "${GET_STATUS}" != "CONFIRMED" ]]; then
  echo -e "${RED}실패 (code=${GET_CODE}, body=${GET_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${GET_STATUS})${NC}"

echo -e "${GREEN}>>>> [v8 Test] 검증 종료 (PASS).${NC}"
