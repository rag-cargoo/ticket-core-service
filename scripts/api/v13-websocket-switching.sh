#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
USER_API="${API_HOST}/api/users"
CONCERT_SETUP_API="${API_HOST}/api/concerts/setup"
CONCERT_API="${API_HOST}/api/concerts"
WS_PUSH_API="${API_HOST}/api/push/websocket"
TS="$(date +%s)"
USER_NAME="ws-user-${TS}"
CONTENT_TYPE="Content-Type: application/json"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

USER_ID=""
CONCERT_ID=""

cleanup() {
    if [[ -n "${USER_ID}" ]]; then
        curl -s -X DELETE "${USER_API}/${USER_ID}" >/dev/null 2>&1 || true
    fi
    if [[ -n "${CONCERT_ID}" ]]; then
        curl -s -X DELETE "${CONCERT_API}/cleanup/${CONCERT_ID}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

echo -e "${BLUE}>>>> [v13 Test] WebSocket subscription API 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 1] 테스트 유저 생성... ${NC}"
USER_RESPONSE="$(curl -s -X POST "${USER_API}" -H "${CONTENT_TYPE}" -d "{\"username\":\"${USER_NAME}\",\"tier\":\"BASIC\"}")"
USER_ID="$(echo "${USER_RESPONSE}" | grep -oP '"id":\s*\K\d+' || true)"
if [[ -z "${USER_ID}" ]]; then
    echo -e "${RED}실패! 유저 생성 실패: ${USER_RESPONSE}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (userId=${USER_ID})${NC}"

echo -ne "${YELLOW}[Step 2] 테스트 공연 생성... ${NC}"
SETUP_RESPONSE="$(curl -s -X POST "${CONCERT_SETUP_API}" -H "${CONTENT_TYPE}" -d "{
  \"title\": \"WS_TEST_${TS}\",
  \"artistName\": \"WS_ARTIST_${TS}\",
  \"agencyName\": \"WS_AGENCY_${TS}\",
  \"concertDate\": \"2026-06-01T18:00:00\",
  \"seatCount\": 30
}")"
CONCERT_ID="$(echo "${SETUP_RESPONSE}" | grep -oP 'ConcertID=\K\d+' || true)"
OPTION_ID="$(echo "${SETUP_RESPONSE}" | grep -oP 'OptionID=\K\d+' || true)"
if [[ -z "${CONCERT_ID}" || -z "${OPTION_ID}" ]]; then
    echo -e "${RED}실패! 공연 생성 실패: ${SETUP_RESPONSE}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (concertId=${CONCERT_ID}, optionId=${OPTION_ID})${NC}"

SEAT_ID="$(curl -s "${CONCERT_API}/options/${OPTION_ID}/seats" | grep -oP '"id":\s*\K\d+' | head -n 1 || true)"
if [[ -z "${SEAT_ID}" ]]; then
    echo -e "${RED}실패! 좌석 조회 실패${NC}"
    exit 1
fi

echo -ne "${YELLOW}[Step 3] WebSocket 대기열 구독 등록... ${NC}"
QUEUE_SUBSCRIBE_RAW="$(curl -s -w '\n%{http_code}' -X POST "${WS_PUSH_API}/waiting-queue/subscriptions" \
    -H "${CONTENT_TYPE}" \
    -d "{\"userId\":${USER_ID},\"concertId\":${CONCERT_ID}}")"
QUEUE_SUBSCRIBE_BODY="$(echo "${QUEUE_SUBSCRIBE_RAW}" | sed '$d')"
QUEUE_SUBSCRIBE_CODE="$(echo "${QUEUE_SUBSCRIBE_RAW}" | tail -n 1)"
EXPECTED_QUEUE_DEST="/topic/waiting-queue/${CONCERT_ID}/${USER_ID}"
if [[ "${QUEUE_SUBSCRIBE_CODE}" != "200" || "${QUEUE_SUBSCRIBE_BODY}" != *"${EXPECTED_QUEUE_DEST}"* ]]; then
    echo -e "${RED}실패! 대기열 구독 등록 실패: ${QUEUE_SUBSCRIBE_BODY} (${QUEUE_SUBSCRIBE_CODE})${NC}"
    exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 4] WebSocket 대기열 구독 해제... ${NC}"
QUEUE_UNSUBSCRIBE_CODE="$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "${WS_PUSH_API}/waiting-queue/subscriptions?userId=${USER_ID}&concertId=${CONCERT_ID}")"
if [[ "${QUEUE_UNSUBSCRIBE_CODE}" != "204" ]]; then
    echo -e "${RED}실패! 대기열 구독 해제 실패: ${QUEUE_UNSUBSCRIBE_CODE}${NC}"
    exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 5] WebSocket 예약 구독 등록... ${NC}"
RESERVATION_SUBSCRIBE_RAW="$(curl -s -w '\n%{http_code}' -X POST "${WS_PUSH_API}/reservations/subscriptions" \
    -H "${CONTENT_TYPE}" \
    -d "{\"userId\":${USER_ID},\"seatId\":${SEAT_ID}}")"
RESERVATION_SUBSCRIBE_BODY="$(echo "${RESERVATION_SUBSCRIBE_RAW}" | sed '$d')"
RESERVATION_SUBSCRIBE_CODE="$(echo "${RESERVATION_SUBSCRIBE_RAW}" | tail -n 1)"
EXPECTED_RES_DEST="/topic/reservations/${SEAT_ID}/${USER_ID}"
if [[ "${RESERVATION_SUBSCRIBE_CODE}" != "200" || "${RESERVATION_SUBSCRIBE_BODY}" != *"${EXPECTED_RES_DEST}"* ]]; then
    echo -e "${RED}실패! 예약 구독 등록 실패: ${RESERVATION_SUBSCRIBE_BODY} (${RESERVATION_SUBSCRIBE_CODE})${NC}"
    exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 6] WebSocket 예약 구독 해제... ${NC}"
RESERVATION_UNSUBSCRIBE_CODE="$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "${WS_PUSH_API}/reservations/subscriptions?userId=${USER_ID}&seatId=${SEAT_ID}")"
if [[ "${RESERVATION_UNSUBSCRIBE_CODE}" != "204" ]]; then
    echo -e "${RED}실패! 예약 구독 해제 실패: ${RESERVATION_UNSUBSCRIBE_CODE}${NC}"
    exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -e "${GREEN}>>>> [v13 Test] 검증 종료 (PASS).${NC}"
