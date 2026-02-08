#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

# --- [통합 설정] ---
API_HOST="${API_HOST:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL:-${API_HOST}/api}"
CONCERT_ID="${CONCERT_ID:-1}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"
USER_API="${USER_API:-${API_HOST}/api/users}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis}"
ACTIVE_KEY_PREFIX="${ACTIVE_KEY_PREFIX:-active-user:}"
USER_ID_VALID=100
USER_ID_BLOCKED=101
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
# ------------------

echo -e "${BLUE}>>>> [v6 Test] 유입량 제어(인터셉터) 검증 시작...${NC}"
FAILED=0

# 1. 가용 좌석 조회 (Step 0)
echo -ne "  [Step 0] 가용 좌석 조회 중... "
SEAT_ID=$(find_available_seat_id "${API_HOST}/api/concerts" || true)
if [ -z "$SEAT_ID" ]; then echo -e "${RED}실패 (좌석 없음)${NC}"; exit 1; fi
echo -e "${GREEN}성공 (Seat: $SEAT_ID)${NC}"

# 2. 실존 유저 생성 (격리 확보)
echo -ne "  [Step 1] 테스트 유저 생성 중... "
USER_ID_VALID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" -d "{\"username\": \"test_v6_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [ -z "$USER_ID_VALID" ]; then echo -e "${RED}실패${NC}"; exit 1; fi
echo -e "${GREEN}성공 (ID: $USER_ID_VALID)${NC}"

# 3. 비활성 유저 생성 (차단 검증용)
echo -ne "  [Step 2] 비활성 유저 생성 중... "
USER_ID_BLOCKED=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" -d "{\"username\": \"test_v6_blocked_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [ -z "$USER_ID_BLOCKED" ]; then echo -e "${RED}실패${NC}"; exit 1; fi
echo -e "${GREEN}성공 (ID: $USER_ID_BLOCKED)${NC}"

# 4. 유저 강제 활성화
echo -ne "  [Step 3] 유저 ${USER_ID_VALID} 강제 활성화... "
docker exec "$REDIS_CONTAINER" redis-cli DEL "${ACTIVE_KEY_PREFIX}${USER_ID_VALID}" "${ACTIVE_KEY_PREFIX}${USER_ID_BLOCKED}" > /dev/null
docker exec "$REDIS_CONTAINER" redis-cli SET "${ACTIVE_KEY_PREFIX}${USER_ID_VALID}" "true" EX 300 > /dev/null
echo -e "${GREEN}완료${NC}"

# 5. 인터셉터 검증: 활성 유저 (성공 예상)
echo -ne "  [Step 4] 활성 유저 요청 테스트... "
RESPONSE=$(curl $CURL_OPTS -H "User-Id: ${USER_ID_VALID}" -X POST "${BASE_URL}/reservations/v1/optimistic" \
     -H "Content-Type: application/json" \
     -d "{\"userId\":${USER_ID_VALID}, \"seatId\":${SEAT_ID}}")
CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$CODE" == "200" ]; then
    echo -e "${GREEN}성공 (Status: 200)${NC}"
else
    echo -e "${RED}실패 (Status: $CODE)${NC}"
    FAILED=1
fi

# 6. 비활성 유저 차단 테스트 (403 예상)
echo -ne "  [Step 5] 비활성 유저(${USER_ID_BLOCKED}) 차단 중... "
RESPONSE=$(curl $CURL_OPTS -H "User-Id: ${USER_ID_BLOCKED}" -X POST "${BASE_URL}/reservations/v1/optimistic" \
     -H "Content-Type: application/json" \
     -d "{\"userId\":${USER_ID_BLOCKED}, \"seatId\":${SEAT_ID}}")
CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$CODE" == "403" ]; then
    echo -e "${GREEN}성공 (Status: 403 Forbidden)${NC}"
else
    echo -e "${RED}실패 (Status: $CODE)${NC}"
    FAILED=1
fi

curl -s -X DELETE "${USER_API}/${USER_ID_VALID}" > /dev/null || true
curl -s -X DELETE "${USER_API}/${USER_ID_BLOCKED}" > /dev/null || true

echo -e "${BLUE}>>>> [v6 Test] 검증 종료.${NC}"

if [[ "$FAILED" -ne 0 ]]; then
    exit 1
fi
