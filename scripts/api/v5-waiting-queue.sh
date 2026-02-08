#!/usr/bin/env bash
set -euo pipefail

# --- [통합 설정] ---
API_HOST="${API_HOST:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL:-${API_HOST}/api/v1/waiting-queue}"
CONCERT_ID="${CONCERT_ID:-1}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
# ------------------

echo -e "${BLUE}>>>> [v5 Test] 대기열 진입 및 순번 검증 시작...${NC}"
FAILED=0

# 1. 5명의 유저 순차 진입
for i in {1..5}
do
    USER_ID=$((100 + i))
    echo -ne "  [User $USER_ID] 대기열 진입 시도... "
    
    RESPONSE=$(curl $CURL_OPTS -X POST "${BASE_URL}/join" \
         -H "Content-Type: application/json" \
         -d "{\"userId\": ${USER_ID}, \"concertId\": ${CONCERT_ID}}")
    
    BODY=$(echo "$RESPONSE" | sed '$d')
    CODE=$(echo "$RESPONSE" | tail -n1)

    if [ "$CODE" == "200" ]; then
        echo -e "${GREEN}성공! (Body: $BODY)${NC}"
    else
        echo -e "${RED}실패! (Status: $CODE, Body: $BODY)${NC}"
        FAILED=1
    fi
done

echo -e "\n>>>> [Step 2] 현재 대기 상태 확인 (Polling)"
for i in {1..5}
do
    USER_ID=$((100 + i))
    echo -ne "  [User $USER_ID] 상태 조회 시도... "
    
    RESPONSE=$(curl $CURL_OPTS -X GET "${BASE_URL}/status?userId=${USER_ID}&concertId=${CONCERT_ID}")
    BODY=$(echo "$RESPONSE" | sed '$d')
    CODE=$(echo "$RESPONSE" | tail -n1)

    if [ "$CODE" == "200" ]; then
        echo -e "${GREEN}성공! (Body: $BODY)${NC}"
    else
        echo -e "${RED}실패! (Status: $CODE, Body: $BODY)${NC}"
        FAILED=1
    fi
done

echo -e "${BLUE}>>>> [v5 Test] 검증 종료.${NC}"

if [[ "$FAILED" -ne 0 ]]; then
    exit 1
fi
