#!/bin/bash
source "$(dirname "$0")/../common/env.sh"
TMP_USERNAME="test_v2_$(date +%s)"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v2] Pessimistic Lock Life-cycle Test${NC}"
echo -e "${BLUE}====================================================${NC}"

# 1. 유저 생성
USER_ID=$(curl -s -X POST "http://127.0.0.1:8080/api/users" -H "${CONTENT_TYPE}" -d "{\"username\": \"${TMP_USERNAME}\"}" | grep -oP '"id":\s*\K\d+')
echo -e "${YELLOW}[Step 1] User Created: ID ${USER_ID}${NC}"

# 2. 예약 테스트
echo -e "${YELLOW}[Step 2] Testing Pessimistic Lock...${NC}"
curl ${CURL_OPTS} -X POST "${BASE_URL}/v2/pessimistic" -H "${CONTENT_TYPE}" -d "{\"userId\": ${USER_ID}, \"seatId\": ${DEFAULT_SEAT_ID}}" -w "\n - Status: %{http_code}\n"

# 3. 삭제
echo -e "${YELLOW}[Step 3] Cleaning up...${NC}"
curl -s -X DELETE "http://127.0.0.1:8080/api/users/${USER_ID}"
echo -e "${BLUE}====================================================${NC}"