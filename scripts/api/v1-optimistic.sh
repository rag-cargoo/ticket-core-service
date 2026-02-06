#!/bin/bash

# ==============================================================================
# [v1] 낙관적 락(Optimistic Lock) 라이프사이클 테스트
# 순서: 임시 유저 생성 -> 예약 시도 -> 유저 삭제(Cleanup)
# ==============================================================================

source "$(dirname "$0")/../common/env.sh"

TMP_USERNAME="test_$(date +%s)"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v1] Optimistic Lock Life-cycle Test${NC}"
echo -e "${BLUE}====================================================${NC}"

# 1. 테스트 유저 생성
echo -e "${YELLOW}[Step 1] Creating temporary user: ${TMP_USERNAME}${NC}"
USER_ID=$(curl -s -X POST "http://127.0.0.1:8080/api/users" \
     -H "${CONTENT_TYPE}" \
     -d "{\"username\": \"${TMP_USERNAME}\"}" | grep -oP '"id":\s*\K\d+')

if [ -z "$USER_ID" ]; then
    echo -e "${RED}Failed to create user. Exiting.${NC}"
    exit 1
fi
echo -e " - User Created! ID: ${USER_ID}"

# 2. 예약 테스트 실행
echo -e "\n${YELLOW}[Step 2] Testing Optimistic Lock Reservation...${NC}"
curl ${CURL_OPTS} -X POST "${BASE_URL}/v1/optimistic" \
     -H "${CONTENT_TYPE}" \
     -H "User-Id: ${USER_ID}" \
     -d "{\"userId\": ${USER_ID}, \"seatId\": ${DEFAULT_SEAT_ID}}" \
     -w "\n - Status Code: %{http_code}\n"

# 3. 데이터 삭제 (Cleanup)
echo -e "\n${YELLOW}[Step 3] Cleaning up temporary user (ID: ${USER_ID})${NC}"
curl -s -X DELETE "http://127.0.0.1:8080/api/users/${USER_ID}"
echo -e " - Done."
echo -e "${BLUE}====================================================${NC}"
