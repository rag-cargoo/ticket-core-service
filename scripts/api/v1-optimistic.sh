#!/bin/bash

# ==============================================================================
# [v1] 낙관적 락(Optimistic Lock) 예약 테스트 스크립트
# ==============================================================================

source "$(dirname "$0")/../common/env.sh"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v1] Optimistic Lock Reservation Test${NC}"
echo -e "${GREEN}[Current Config]${NC}"
echo -e " - Target URL: ${BASE_URL}/v1/optimistic"
echo -e " - Test User:  ID=${DEFAULT_USER_ID}"
echo -e " - Test Seat:  ID=${DEFAULT_SEAT_ID}"
echo -e "${BLUE}====================================================${NC}"

# CURL_OPTS를 추가하여 무한 대기를 방지
curl ${CURL_OPTS} -X POST "${BASE_URL}/v1/optimistic" \
     -H "${CONTENT_TYPE}" \
     -d "{\"userId\": ${DEFAULT_USER_ID}, \"seatId\": ${DEFAULT_SEAT_ID}}" \
     -w "\n\n${GREEN}Result Status: %{http_code}${NC}\n"