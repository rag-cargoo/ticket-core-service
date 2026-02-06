#!/bin/bash
# --- [통합 설정] ---
BASE_URL="http://127.0.0.1:8080/api/reservations"
CONCERT_API="http://127.0.0.1:8080/api/concerts"
USER_API="http://127.0.0.1:8080/api/users"
CONTENT_TYPE="Content-Type: application/json"
CURL_OPTS="--connect-timeout 5 --max-time 10 -s"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
GREEN='\033[0;32m'
# ------------------

echo -e "${BLUE}>>>> [v3 Test] 분산 락 검증 시작...${NC}"

# 1. 가용 좌석 실시간 조회 (격리 확보)
echo -ne "${YELLOW}[Step 0] 가용 좌석 조회 중... ${NC}"
SEAT_ID=$(curl -s "${CONCERT_API}/options/1/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1)
if [ -z "$SEAT_ID" ]; then
    SEAT_ID=$(curl -s "${CONCERT_API}/options/2/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1)
fi
if [ -z "$SEAT_ID" ]; then echo -e "${RED}실패! (가용 좌석 없음)${NC}"; exit 1; fi
echo -e "${GREEN}성공! (Seat ID: $SEAT_ID)${NC}"

# 2. 테스트 유저 생성
echo -ne "${YELLOW}[Step 1] 테스트 유저 생성 중... ${NC}"
USER_ID=$(curl $CURL_OPTS -X POST "$USER_API" -H "$CONTENT_TYPE" -d "{\"username\": \"test_v3_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+')
if [ -z "$USER_ID" ]; then echo -e "${RED}실패!${NC}"; exit 1; fi
echo -e "${GREEN}성공! (ID: $USER_ID)${NC}"

# 3. Redis 강제 활성화
docker exec redis redis-cli SET "active-user:${USER_ID}" "true" EX 300 > /dev/null

# 4. 예약 테스트 실행 (v3 - Distributed Lock)
echo -e "${YELLOW}[Step 3] 분산 락 예약 API 호출 (Seat: ${SEAT_ID})...${NC}"
RESPONSE=$(curl $CURL_OPTS -w "\n%{http_code}" -X POST "${BASE_URL}/v3/distributed-lock" \
     -H "${CONTENT_TYPE}" \
     -H "User-Id: ${USER_ID}" \
     -d "{\"userId\": ${USER_ID}, \"seatId\": ${SEAT_ID}}")

CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$CODE" == "200" ]; then echo -e "${GREEN}>> [SUCCESS] 예약 성공!${NC}"; else echo -e "${RED}>> [FAIL] 예약 실패! (Status: $CODE)${NC}"; fi

# 5. 데이터 삭제 (Cleanup)
curl $CURL_OPTS -X DELETE "${USER_API}/${USER_ID}" > /dev/null
echo -e "${BLUE}>>>> [v3 Test] 종료.${NC}"
