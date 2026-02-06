#!/bin/bash

# --- [통합 설정] ---
BASE_URL="http://localhost:8080/api"
CONCERT_ID=1
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"
USER_ID_VALID=100
USER_ID_INVALID=999
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
# ------------------

echo -e "${BLUE}>>>> [v6 Test] 유입량 제어(인터셉터) 검증 시작...${NC}"

# 1. 가용 좌석 조회 (Step 0)
echo -ne "  [Step 0] 가용 좌석 조회 중... "
SEAT_ID=$(curl -s "http://localhost:8080/api/concerts/options/1/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1)
if [ -z "$SEAT_ID" ]; then echo -e "${RED}실패 (좌석 없음)${NC}"; exit 1; fi
echo -e "${GREEN}성공 (Seat: $SEAT_ID)${NC}"

# 2. 실존 유저 생성 (격리 확보)
echo -ne "  [Step 1] 테스트 유저 생성 중... "
USER_ID_VALID=$(curl -s -X POST "http://localhost:8080/api/users" -H "Content-Type: application/json" -d "{\"username\": \"test_v6_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+')
if [ -z "$USER_ID_VALID" ]; then echo -e "${RED}실패${NC}"; exit 1; fi
echo -e "${GREEN}성공 (ID: $USER_ID_VALID)${NC}"

# 3. 유저 강제 활성화
echo -ne "  [Step 2] 유저 ${USER_ID_VALID} 강제 활성화... "
docker exec redis redis-cli SET "active-user:${USER_ID_VALID}" "true" EX 300 > /dev/null
echo -e "${GREEN}완료${NC}"

# 4. 인터셉터 검증: 활성 유저 (성공 예상)
echo -ne "  [Step 3] 활성 유저 요청 테스트... "
RESPONSE=$(curl $CURL_OPTS -H "User-Id: ${USER_ID_VALID}" -X POST "${BASE_URL}/reservations/v1/optimistic" \
     -H "Content-Type: application/json" \
     -d "{\"userId\":${USER_ID_VALID}, \"seatId\":${SEAT_ID}}")
CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$CODE" == "200" ]; then echo -e "${GREEN}성공 (Status: 200)${NC}"; else echo -e "${RED}실패 (Status: $CODE)${NC}"; fi

# 4. 비활성 유저 차단 테스트 (403 예상)
echo -ne "  [Step 3] 비활성 유저(999) 차단 중... "
RESPONSE=$(curl $CURL_OPTS -H "User-Id: ${USER_ID_INVALID}" -X POST "${BASE_URL}/reservations/v1/optimistic" \
     -H "Content-Type: application/json" \
     -d "{\"userId\":${USER_ID_INVALID}, \"seatId\":${SEAT_ID}}")
CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$CODE" == "403" ]; then echo -e "${GREEN}성공 (Status: 403 Forbidden)${NC}"; else echo -e "${RED}실패 (Status: $CODE)${NC}"; fi

echo -e "${BLUE}>>>> [v6 Test] 검증 종료.${NC}"