#!/bin/bash
source "$(dirname "$0")/../common/env.sh"
TMP_USERNAME="test_v3_$(date +%s)"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v3] Distributed Lock Life-cycle Test${NC}"
echo -e "${BLUE}====================================================${NC}"

# 1. 유저 생성
USER_ID=$(curl -s -X POST "http://localhost:8080/api/users" -H "${CONTENT_TYPE}" -d "{\"username\": \"${TMP_USERNAME}\"}" | grep -oP '"id":\s*\K\d+')
echo -e "${YELLOW}[Step 1] User Created: ID ${USER_ID}${NC}"

# 2. 예약 테스트
echo -e "${YELLOW}[Step 2] Testing Distributed Lock...${NC}"
curl ${CURL_OPTS} -X POST "${BASE_URL}/v3/distributed-lock" -H "${CONTENT_TYPE}" -d "{\"userId\": ${USER_ID}, \"seatId\": ${DEFAULT_SEAT_ID}}" -w "\n - Status: %{http_code}\n"

# 3. 삭제
echo -e "${YELLOW}[Step 3] Cleaning up...${NC}"
curl -s -X DELETE "http://localhost:8080/api/users/${USER_ID}"
echo -e "${BLUE}====================================================${NC}"