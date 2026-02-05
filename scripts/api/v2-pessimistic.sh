#!/bin/bash

# ==============================================================================
# [v2] 비관적 락(Pessimistic Lock) 예약 테스트 스크립트
#
# 목적: DB SELECT FOR UPDATE 기능을 이용한 순차 처리 API를 호출합니다.
# 설정 안내:
# - 서버 주소 및 기본 데이터(User/Seat ID)는 'scripts/common/env.sh'에서 수정하세요.
# ==============================================================================

source "$(dirname "$0")/../common/env.sh"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v2] Pessimistic Lock Reservation Test${NC}"
echo -e "${GREEN}[Current Config]${NC}"
echo -e " - Target URL: ${BASE_URL}/v2/pessimistic"
echo -e " - Test User:  ID=${DEFAULT_USER_ID}"
echo -e " - Test Seat:  ID=${DEFAULT_SEAT_ID}"
echo -e "${BLUE}====================================================${NC}"

curl -s -X POST "${BASE_URL}/v2/pessimistic" \
     -H "${CONTENT_TYPE}" \
     -d "{\"userId\": ${DEFAULT_USER_ID}, \"seatId\": ${DEFAULT_SEAT_ID}}" \
     -w "\n\n${GREEN}Result Status: %{http_code}${NC}\n"
