#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

# --- [통합 설정] ---
API_HOST="${API_HOST:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL:-${API_HOST}/api/reservations}"
CONCERT_API="${CONCERT_API:-${API_HOST}/api/concerts}"
USER_API="${USER_API:-${API_HOST}/api/users}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis}"
ACTIVE_KEY_PREFIX="${ACTIVE_KEY_PREFIX:-active-user:}"
CONTENT_TYPE="Content-Type: application/json"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
# ------------------

echo -e "${BLUE}>>>> [v4 Test] Kafka 비동기 검증 시작...${NC}"

USER_ID=""
cleanup() {
    if [[ -n "${USER_ID}" ]]; then
        curl -s -X DELETE "${USER_API}/${USER_ID}" > /dev/null || true
    fi
}
trap cleanup EXIT

# 1. 가용 좌석 조회
echo -ne "${YELLOW}[Step 0] 가용 좌석 조회 중... ${NC}"
SEAT_ID=$(find_available_seat_id "${CONCERT_API}" || true)
if [ -z "$SEAT_ID" ]; then echo -e "${RED}실패! (가용 좌석 없음)${NC}"; exit 1; fi
echo -e "${GREEN}성공! (Seat ID: $SEAT_ID)${NC}"

# 2. 테스트 유저 생성 + 강제 활성화
echo -ne "${YELLOW}[Step 1] 테스트 유저 생성 중... ${NC}"
USER_ID=$(curl -s -X POST "$USER_API" -H "$CONTENT_TYPE" -d "{\"username\": \"test_v4_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [ -z "$USER_ID" ]; then echo -e "${RED}실패!${NC}"; exit 1; fi
echo -e "${GREEN}성공! (ID: $USER_ID)${NC}"

docker exec "$REDIS_CONTAINER" redis-cli SET "${ACTIVE_KEY_PREFIX}${USER_ID}" "true" EX 300 > /dev/null

# 3. 예약 요청 (Enqueued)
echo -e "${YELLOW}[Step 2] 비동기 예약 요청 전송 (Seat: $SEAT_ID)...${NC}"
REQUEST_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v4-opt/queue-polling" \
     -H "${CONTENT_TYPE}" \
     -H "User-Id: ${USER_ID}" \
     -d "{\"userId\": ${USER_ID}, \"seatId\": ${SEAT_ID}}")
if [[ "$REQUEST_CODE" != "202" ]]; then
    echo -e "${RED}실패! 비동기 예약 enqueue 실패 (Status: ${REQUEST_CODE})${NC}"
    exit 1
fi

echo -e "\n${YELLOW}[Step 3] 처리 상태 폴링 중...${NC}"
SUCCESS="false"
for i in {1..10}
do
    STATUS_JSON=$(curl -s "${BASE_URL}/v4/status?userId=${USER_ID}&seatId=${SEAT_ID}")
    STATUS=$(echo "$STATUS_JSON" | grep -oP '"status":"\K[^"]+' || true)
    echo -e "  - 시도 $i: 현재 상태 -> ${GREEN}${STATUS}${NC}"
    if [ "$STATUS" == "SUCCESS" ]; then
        echo -e "\n${GREEN}>> [SUCCESS] Kafka 비동기 처리 완료!${NC}"
        SUCCESS="true"
        break
    fi
    sleep 2
done

if [[ "$SUCCESS" != "true" ]]; then
    echo -e "${RED}>> [FAIL] 제한 시간 내 SUCCESS 상태를 확인하지 못함${NC}"
    exit 1
fi

echo -e "${BLUE}>>>> [v4 Test] 종료.${NC}"
