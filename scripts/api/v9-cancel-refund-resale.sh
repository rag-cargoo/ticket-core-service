#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
RESERVATION_API="${RESERVATION_API:-${API_HOST}/api/reservations/v6}"
WAITING_QUEUE_API="${WAITING_QUEUE_API:-${API_HOST}/api/v1/waiting-queue}"
USER_API="${USER_API:-${API_HOST}/api/users}"
CONCERT_API="${CONCERT_API:-${API_HOST}/api/concerts}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis}"
QUEUE_KEY_PREFIX="${QUEUE_KEY_PREFIX:-waiting-queue:}"
ACTIVE_KEY_PREFIX="${ACTIVE_KEY_PREFIX:-active-user:}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}>>>> [v9 Test] Step 10 취소/환불/재판매 대기열 연계 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 0] 테스트 유저 2명 생성... ${NC}"
OWNER_USER_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" \
  -d "{\"username\":\"test_v9_owner_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
WAITING_USER_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" \
  -d "{\"username\":\"test_v9_waiting_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [[ -z "${OWNER_USER_ID}" || -z "${WAITING_USER_ID}" ]]; then
  echo -e "${RED}실패${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (owner=${OWNER_USER_ID}, waiting=${WAITING_USER_ID})${NC}"

echo -ne "${YELLOW}[Step 1] 대상 콘서트 식별... ${NC}"
CONCERT_ID=$(curl -s "${CONCERT_API}" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
if [[ -z "${CONCERT_ID}" ]]; then
  ensure_test_data || true
  CONCERT_ID=$(curl -s "${CONCERT_API}" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
fi
if [[ -z "${CONCERT_ID}" ]]; then
  echo -e "${RED}실패 (concert 없음)${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (concertId=${CONCERT_ID})${NC}"

echo -ne "${YELLOW}[Step 2] 일반 판매 가능 정책 보정... ${NC}"
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

echo -ne "${YELLOW}[Step 3] 대기열/활성 키 정리... ${NC}"
docker exec "${REDIS_CONTAINER}" redis-cli ZREM "${QUEUE_KEY_PREFIX}${CONCERT_ID}" "${WAITING_USER_ID}" >/dev/null 2>&1 || true
docker exec "${REDIS_CONTAINER}" redis-cli DEL "${ACTIVE_KEY_PREFIX}${WAITING_USER_ID}" >/dev/null 2>&1 || true
echo -e "${GREEN}완료${NC}"

echo -ne "${YELLOW}[Step 4] 대기 유저 큐 진입... ${NC}"
JOIN_RESPONSE=$(curl ${CURL_OPTS} -X POST "${WAITING_QUEUE_API}/join" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${WAITING_USER_ID},\"concertId\":${CONCERT_ID}}")
JOIN_BODY=$(echo "${JOIN_RESPONSE}" | sed '$d')
JOIN_CODE=$(echo "${JOIN_RESPONSE}" | tail -n1)
JOIN_STATUS=$(echo "${JOIN_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${JOIN_CODE}" != "200" || "${JOIN_STATUS}" != "WAITING" ]]; then
  echo -e "${RED}실패 (code=${JOIN_CODE}, body=${JOIN_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${JOIN_STATUS})${NC}"

echo -ne "${YELLOW}[Step 5] 가용 좌석 조회... ${NC}"
SEAT_ID=$(find_available_seat_id "${CONCERT_API}" || true)
if [[ -z "${SEAT_ID}" ]]; then
  echo -e "${RED}실패 (available seat 없음)${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (seatId=${SEAT_ID})${NC}"

echo -ne "${YELLOW}[Step 6] CONFIRMED 예약 생성(HOLD -> PAYING -> CONFIRMED)... ${NC}"
HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/holds" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${OWNER_USER_ID},\"seatId\":${SEAT_ID}}")
HOLD_BODY=$(echo "${HOLD_RESPONSE}" | sed '$d')
HOLD_CODE=$(echo "${HOLD_RESPONSE}" | tail -n1)
RESERVATION_ID=$(echo "${HOLD_BODY}" | grep -oP '"id":\s*\K\d+' || true)
HOLD_STATUS=$(echo "${HOLD_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${HOLD_CODE}" != "201" || "${HOLD_STATUS}" != "HOLD" || -z "${RESERVATION_ID}" ]]; then
  echo -e "${RED}실패(HOLD) code=${HOLD_CODE}, body=${HOLD_BODY}${NC}"
  exit 1
fi

PAYING_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/${RESERVATION_ID}/paying" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${OWNER_USER_ID}}")
PAYING_BODY=$(echo "${PAYING_RESPONSE}" | sed '$d')
PAYING_CODE=$(echo "${PAYING_RESPONSE}" | tail -n1)
PAYING_STATUS=$(echo "${PAYING_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${PAYING_CODE}" != "200" || "${PAYING_STATUS}" != "PAYING" ]]; then
  echo -e "${RED}실패(PAYING) code=${PAYING_CODE}, body=${PAYING_BODY}${NC}"
  exit 1
fi

CONFIRM_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/${RESERVATION_ID}/confirm" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${OWNER_USER_ID}}")
CONFIRM_BODY=$(echo "${CONFIRM_RESPONSE}" | sed '$d')
CONFIRM_CODE=$(echo "${CONFIRM_RESPONSE}" | tail -n1)
CONFIRM_STATUS=$(echo "${CONFIRM_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${CONFIRM_CODE}" != "200" || "${CONFIRM_STATUS}" != "CONFIRMED" ]]; then
  echo -e "${RED}실패(CONFIRMED) code=${CONFIRM_CODE}, body=${CONFIRM_BODY}${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (reservationId=${RESERVATION_ID})${NC}"

echo -ne "${YELLOW}[Step 7] 예약 취소 + 재판매 대기열 연계... ${NC}"
CANCEL_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/${RESERVATION_ID}/cancel" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${OWNER_USER_ID}}")
CANCEL_BODY=$(echo "${CANCEL_RESPONSE}" | sed '$d')
CANCEL_CODE=$(echo "${CANCEL_RESPONSE}" | tail -n1)
CANCEL_STATUS=$(echo "${CANCEL_BODY}" | grep -oP '"status":"\K[^"]+' || true)
ACTIVATED_USER_ID=$(echo "${CANCEL_BODY}" | grep -oP '"resaleActivatedUserIds":\[\K\d+' || true)
if [[ "${CANCEL_CODE}" != "200" || "${CANCEL_STATUS}" != "CANCELLED" || "${ACTIVATED_USER_ID}" != "${WAITING_USER_ID}" ]]; then
  echo -e "${RED}실패 (code=${CANCEL_CODE}, body=${CANCEL_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (cancelled, activatedUser=${ACTIVATED_USER_ID})${NC}"

echo -ne "${YELLOW}[Step 8] 대기 유저 ACTIVE 전환 확인... ${NC}"
QUEUE_STATUS_RESPONSE=$(curl ${CURL_OPTS} "${WAITING_QUEUE_API}/status?userId=${WAITING_USER_ID}&concertId=${CONCERT_ID}")
QUEUE_STATUS_BODY=$(echo "${QUEUE_STATUS_RESPONSE}" | sed '$d')
QUEUE_STATUS_CODE=$(echo "${QUEUE_STATUS_RESPONSE}" | tail -n1)
QUEUE_STATUS=$(echo "${QUEUE_STATUS_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${QUEUE_STATUS_CODE}" != "200" || "${QUEUE_STATUS}" != "ACTIVE" ]]; then
  echo -e "${RED}실패 (code=${QUEUE_STATUS_CODE}, body=${QUEUE_STATUS_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${QUEUE_STATUS})${NC}"

echo -ne "${YELLOW}[Step 9] 환불 완료 처리... ${NC}"
REFUND_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/${RESERVATION_ID}/refund" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${OWNER_USER_ID}}")
REFUND_BODY=$(echo "${REFUND_RESPONSE}" | sed '$d')
REFUND_CODE=$(echo "${REFUND_RESPONSE}" | tail -n1)
REFUND_STATUS=$(echo "${REFUND_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${REFUND_CODE}" != "200" || "${REFUND_STATUS}" != "REFUNDED" ]]; then
  echo -e "${RED}실패 (code=${REFUND_CODE}, body=${REFUND_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${REFUND_STATUS})${NC}"

echo -ne "${YELLOW}[Step 10] 최종 상태 조회... ${NC}"
GET_RESPONSE=$(curl ${CURL_OPTS} "${RESERVATION_API}/${RESERVATION_ID}?userId=${OWNER_USER_ID}")
GET_BODY=$(echo "${GET_RESPONSE}" | sed '$d')
GET_CODE=$(echo "${GET_RESPONSE}" | tail -n1)
GET_STATUS=$(echo "${GET_BODY}" | grep -oP '"status":"\K[^"]+' || true)
if [[ "${GET_CODE}" != "200" || "${GET_STATUS}" != "REFUNDED" ]]; then
  echo -e "${RED}실패 (code=${GET_CODE}, body=${GET_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (status=${GET_STATUS})${NC}"

curl -s -X DELETE "${USER_API}/${OWNER_USER_ID}" >/dev/null 2>&1 || true
curl -s -X DELETE "${USER_API}/${WAITING_USER_ID}" >/dev/null 2>&1 || true

echo -e "${GREEN}>>>> [v9 Test] 검증 종료 (PASS).${NC}"
