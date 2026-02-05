#!/bin/bash

# ==============================================================================
# [v3] Redis 분산 락(Distributed Lock) 예약 테스트 스크립트
#
# 목적: Redisson 라이브러리를 이용한 분산 환경 동시성 제어 API를 호출합니다.
# 설정 안내:
# - 서버 주소 및 기본 데이터(User/Seat ID)는 'scripts/common/env.sh'에서 수정하세요.
# ==============================================================================

source "$(dirname "$0")/../common/env.sh"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v3] Distributed Lock Reservation Test${NC}"
echo -e "${GREEN}[Current Config]${NC}"
echo -e " - Target URL: ${BASE_URL}/v3/distributed-lock"
echo -e " - Test User:  ID=${DEFAULT_USER_ID}"
echo -e " - Test Seat:  ID=${DEFAULT_SEAT_ID}"
echo -e "${BLUE}====================================================${NC}"

curl -s -X POST "${BASE_URL}/v3/distributed-lock" \
     -H "${CONTENT_TYPE}" \
     -d "{\"userId\": ${DEFAULT_USER_ID}, \"seatId\": ${DEFAULT_SEAT_ID}}" \
     -w "\n\n${GREEN}Result Status: %{http_code}${NC}\n"
