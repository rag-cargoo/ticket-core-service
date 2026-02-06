#!/bin/bash
# --- [통합 설정] ---
BASE_URL="http://127.0.0.1:8080/api/reservations"
CONCERT_API="http://127.0.0.1:8080/api/concerts"
CONTENT_TYPE="Content-Type: application/json"
USER_ID=1
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
# ------------------

echo -e "${BLUE}>>>> [v4 Test] Kafka 비동기 검증 시작...${NC}"

# 1. 가용 좌석 조회
echo -ne "${YELLOW}[Step 0] 가용 좌석 조회 중... ${NC}"
SEAT_ID=$(curl -s "${CONCERT_API}/options/1/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1)
if [ -z "$SEAT_ID" ]; then
    SEAT_ID=$(curl -s "${CONCERT_API}/options/2/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1)
fi
if [ -z "$SEAT_ID" ]; then echo -e "${RED}실패! (가용 좌석 없음)${NC}"; exit 1; fi
echo -e "${GREEN}성공! (Seat ID: $SEAT_ID)${NC}"

# 2. 강제 활성화 (테스트용)
docker exec redis redis-cli SET "active-user:${USER_ID}" "true" EX 300 > /dev/null

# 3. 예약 요청 (Enqueued)
echo -e "${YELLOW}[Step 1] 비동기 예약 요청 전송 (Seat: $SEAT_ID)...${NC}"
curl -s -X POST "${BASE_URL}/v4-opt/queue-polling" \
     -H "${CONTENT_TYPE}" \
     -H "User-Id: ${USER_ID}" \
     -d "{\"userId\": ${USER_ID}, \"seatId\": ${SEAT_ID}}"

echo -e "\n${YELLOW}[Step 2] 처리 상태 폴링 중...${NC}"
for i in {1..10}
do
    STATUS_JSON=$(curl -s "${BASE_URL}/v4/status?userId=${USER_ID}&seatId=${SEAT_ID}")
    STATUS=$(echo "$STATUS_JSON" | grep -oP '"status":"\K[^"]+')
    echo -e "  - 시도 $i: 현재 상태 -> ${GREEN}${STATUS}${NC}"
    if [ "$STATUS" == "SUCCESS" ]; then
        echo -e "\n${GREEN}>> [SUCCESS] Kafka 비동기 처리 완료!${NC}"
        break
    fi
    sleep 2
done
echo -e "${BLUE}>>>> [v4 Test] 종료.${NC}"
