#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL:-${API_HOST}/api/v1/waiting-queue}"
CONCERT_ID="${CONCERT_ID:-1}"
USER_ID="${USER_ID:-$((700000 + $(date +%s) % 100000))}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis}"
QUEUE_KEY_PREFIX="${QUEUE_KEY_PREFIX:-waiting-queue:}"
ACTIVE_KEY_PREFIX="${ACTIVE_KEY_PREFIX:-active-user:}"
SSE_TIMEOUT_SEC="${SSE_TIMEOUT_SEC:-25}"

CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

cleanup() {
    docker exec "$REDIS_CONTAINER" redis-cli ZREM "${QUEUE_KEY_PREFIX}${CONCERT_ID}" "${USER_ID}" >/dev/null 2>&1 || true
    docker exec "$REDIS_CONTAINER" redis-cli DEL "${ACTIVE_KEY_PREFIX}${USER_ID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo -e "${BLUE}>>>> [v7 Test] SSE 순번 자동 푸시 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 0] 테스트 키 초기화 중... ${NC}"
docker exec "$REDIS_CONTAINER" redis-cli DEL "${QUEUE_KEY_PREFIX}${CONCERT_ID}" >/dev/null
docker exec "$REDIS_CONTAINER" redis-cli DEL "${ACTIVE_KEY_PREFIX}${USER_ID}" >/dev/null
echo -e "${GREEN}완료${NC}"

echo -ne "${YELLOW}[Step 1] 대기열 진입 요청 (User: ${USER_ID})... ${NC}"
JOIN_RESPONSE=$(curl $CURL_OPTS -X POST "${BASE_URL}/join" \
     -H "Content-Type: application/json" \
     -d "{\"userId\":${USER_ID},\"concertId\":${CONCERT_ID}}")
JOIN_BODY=$(echo "$JOIN_RESPONSE" | sed '$d')
JOIN_CODE=$(echo "$JOIN_RESPONSE" | tail -n1)
JOIN_STATUS=$(echo "$JOIN_BODY" | grep -oP '"status":"\K[^"]+' || true)

if [[ "$JOIN_CODE" != "200" ]]; then
    echo -e "${RED}실패! (Status: ${JOIN_CODE}, Body: ${JOIN_BODY})${NC}"
    exit 1
fi
echo -e "${GREEN}성공! (status=${JOIN_STATUS:-unknown})${NC}"

echo -e "${YELLOW}[Step 2] SSE 구독 시작 (timeout=${SSE_TIMEOUT_SEC}s)...${NC}"
SSE_OUTPUT=$(curl -sN --max-time "${SSE_TIMEOUT_SEC}" \
    -H "Accept: text/event-stream" \
    "${BASE_URL}/subscribe?userId=${USER_ID}&concertId=${CONCERT_ID}" || true)

INIT_COUNT=$(grep -c '^event:INIT' <<< "$SSE_OUTPUT" || true)
RANK_COUNT=$(grep -c '^event:RANK_UPDATE' <<< "$SSE_OUTPUT" || true)
ACTIVE_COUNT=$(grep -c '^event:ACTIVE' <<< "$SSE_OUTPUT" || true)
KEEPALIVE_COUNT=$(grep -c '^event:KEEPALIVE' <<< "$SSE_OUTPUT" || true)

echo -e "  - INIT: ${INIT_COUNT}, RANK_UPDATE: ${RANK_COUNT}, ACTIVE: ${ACTIVE_COUNT}, KEEPALIVE: ${KEEPALIVE_COUNT}"

if [[ "$INIT_COUNT" -lt 1 ]]; then
    echo -e "${RED}실패! INIT 이벤트 미수신${NC}"
    echo "$SSE_OUTPUT" | tail -n 40
    exit 1
fi

if [[ "$RANK_COUNT" -lt 1 && "$ACTIVE_COUNT" -lt 1 ]]; then
    echo -e "${RED}실패! RANK_UPDATE/ACTIVE 이벤트 미수신${NC}"
    echo "$SSE_OUTPUT" | tail -n 40
    exit 1
fi

if [[ "$ACTIVE_COUNT" -lt 1 ]]; then
    echo -e "${RED}실패! ACTIVE 이벤트 미수신 (SSE 자동 푸시 확인 실패)${NC}"
    echo "$SSE_OUTPUT" | tail -n 40
    exit 1
fi

echo -ne "${YELLOW}[Step 3] 최종 상태 확인... ${NC}"
STATUS_JSON=$(curl -s "${BASE_URL}/status?userId=${USER_ID}&concertId=${CONCERT_ID}")
FINAL_STATUS=$(echo "$STATUS_JSON" | grep -oP '"status":"\K[^"]+' || true)
if [[ "$FINAL_STATUS" != "ACTIVE" ]]; then
    echo -e "${RED}실패! 최종 상태=${FINAL_STATUS:-unknown}, body=${STATUS_JSON}${NC}"
    exit 1
fi
echo -e "${GREEN}성공! 최종 상태 ACTIVE${NC}"

echo -e "${GREEN}>>>> [v7 Test] 검증 종료 (PASS).${NC}"
