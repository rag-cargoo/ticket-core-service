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
CURL_OPTS="--connect-timeout 5 --max-time 10 -s"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
# ------------------

echo -e "${BLUE}>>>> [v1 Test] 낙관적 락 검증 시작...${NC}"

USER_ID=""
cleanup() {
    if [[ -n "${USER_ID}" ]]; then
        curl $CURL_OPTS -X DELETE "${USER_API}/${USER_ID}" > /dev/null || true
    fi
}
trap cleanup EXIT

# 1. 가용 좌석 실시간 조회 (지능화)
echo -ne "${YELLOW}[Step 0] 가용 좌석 조회 중... ${NC}"
SEAT_ID=$(find_available_seat_id "${CONCERT_API}" || true)
if [ -z "$SEAT_ID" ]; then echo -e "${RED}실패! (가용 좌석 없음)${NC}"; exit 1; fi
echo -e "${GREEN}성공! (Seat ID: $SEAT_ID)${NC}"

# 2. 테스트 유저 생성
echo -ne "${YELLOW}[Step 1] 테스트 유저 생성 중... ${NC}"
USER_ID=$(curl $CURL_OPTS -X POST "$USER_API" -H "$CONTENT_TYPE" -d "{\"username\": \"test_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [ -z "$USER_ID" ]; then echo -e "${RED}실패!${NC}"; exit 1; fi
echo -e "${GREEN}성공! (ID: $USER_ID)${NC}"

# 3. Redis 강제 활성화
docker exec "$REDIS_CONTAINER" redis-cli SET "${ACTIVE_KEY_PREFIX}${USER_ID}" "true" EX 300 > /dev/null

# 4. 예약 요청
echo -e "${YELLOW}[Step 3] 예약 API 호출 (Seat: $SEAT_ID)...${NC}"
RESPONSE=$(curl $CURL_OPTS -w "\n%{http_code}" -X POST "${BASE_URL}/v1/optimistic" \
     -H "$CONTENT_TYPE" \
     -H "User-Id: ${USER_ID}" \
     -d "{\"userId\": ${USER_ID}, \"seatId\": ${SEAT_ID}}")

CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$CODE" == "200" ]; then
    echo -e "${GREEN}>> [SUCCESS] 예약 성공!${NC}"
else
    echo -e "${RED}>> [FAIL] 예약 실패! (Status: $CODE)${NC}"
    exit 1
fi

echo -e "${BLUE}>>>> [v1 Test] 종료.${NC}"
