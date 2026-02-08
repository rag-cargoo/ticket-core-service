#!/usr/bin/env bash
set -euo pipefail

# --- [통합 설정] ---
API_HOST="${API_HOST:-http://127.0.0.1:8080}"
SETUP_API="${SETUP_API:-${API_HOST}/api/concerts/setup}"
CONTENT_TYPE="Content-Type: application/json"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
# ------------------

CONCERT_TITLE="TEST_CONCERT_$(date +%s)"
CONCERT_DATE=$(date -d "+10 days" +"%Y-%m-%dT%H:%M:%S")

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[Admin] Setting up new test environment...${NC}"
echo -e "${BLUE}====================================================${NC}"

# 1. 공연 및 좌석 생성
RESPONSE=$(curl -s -X POST "${SETUP_API}" \
     -H "${CONTENT_TYPE}" \
     -d "{
       \"title\": \"${CONCERT_TITLE}\",
       \"artistName\": \"NewJeans\",
       \"agencyName\": \"ADOR\",
       \"concertDate\": \"${CONCERT_DATE}\",
       \"seatCount\": 50
     }")

echo -e "Response: ${RESPONSE}"

# 생성된 ID 추출
CONCERT_ID=$(echo "$RESPONSE" | grep -oP 'ConcertID=\K\d+' || true)
OPTION_ID=$(echo "$RESPONSE" | grep -oP 'OptionID=\K\d+' || true)

if [ -z "$OPTION_ID" ]; then
    echo -e "${RED}Failed to setup environment.${NC}"
    exit 1
fi

echo -e "${GREEN}Ready for testing! Concert: ${CONCERT_ID}, Option: ${OPTION_ID}${NC}"
echo -e "${BLUE}====================================================${NC}"
