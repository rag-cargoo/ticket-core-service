#!/usr/bin/env bash
set -euo pipefail

# --- [통합 설정] ---
API_HOST="${API_HOST:-http://127.0.0.1:8080}"
SETUP_API="${SETUP_API:-${API_HOST}/api/concerts/setup}"
AGENCY_API="${AGENCY_API:-${API_HOST}/api/agencies}"
ARTIST_API="${ARTIST_API:-${API_HOST}/api/artists}"
USE_DOMAIN_CRUD="${USE_DOMAIN_CRUD:-1}"
CONTENT_TYPE="Content-Type: application/json"
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
# ------------------

CONCERT_TITLE="TEST_CONCERT_$(date +%s)"
CONCERT_DATE=$(date -d "+10 days" +"%Y-%m-%dT%H:%M:%S")
SUFFIX=$(date +%s)
AGENCY_NAME="TEST_AGENCY_${SUFFIX}"
ARTIST_NAME="TEST_ARTIST_${SUFFIX}"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[Admin] Setting up new test environment...${NC}"
echo -e "${BLUE}====================================================${NC}"

if [ "${USE_DOMAIN_CRUD}" = "1" ]; then
    echo -e "${YELLOW}Step 0) Creating agency/artist via CRUD APIs...${NC}"

    AGENCY_RESPONSE=$(curl -s -X POST "${AGENCY_API}" \
         -H "${CONTENT_TYPE}" \
         -d "{
           \"name\": \"${AGENCY_NAME}\",
           \"countryCode\": \"KR\",
           \"homepageUrl\": \"https://example.com/${AGENCY_NAME}\"
         }")
    AGENCY_ID=$(echo "${AGENCY_RESPONSE}" | grep -oP '"id":\s*\K\d+' || true)
    if [ -z "${AGENCY_ID}" ]; then
        echo -e "${RED}Failed to create agency via CRUD API.${NC}"
        echo "Response: ${AGENCY_RESPONSE}"
        exit 1
    fi

    ARTIST_RESPONSE=$(curl -s -X POST "${ARTIST_API}" \
         -H "${CONTENT_TYPE}" \
         -d "{
           \"name\": \"${ARTIST_NAME}\",
           \"agencyId\": ${AGENCY_ID},
           \"displayName\": \"${ARTIST_NAME}\",
           \"genre\": \"K-POP\",
           \"debutDate\": \"2022-07-22\"
         }")
    ARTIST_ID=$(echo "${ARTIST_RESPONSE}" | grep -oP '"id":\s*\K\d+' || true)
    if [ -z "${ARTIST_ID}" ]; then
        echo -e "${RED}Failed to create artist via CRUD API.${NC}"
        echo "Response: ${ARTIST_RESPONSE}"
        exit 1
    fi
fi

# 1. 공연 및 좌석 생성
RESPONSE=$(curl -s -X POST "${SETUP_API}" \
     -H "${CONTENT_TYPE}" \
     -d "{
       \"title\": \"${CONCERT_TITLE}\",
       \"artistName\": \"${ARTIST_NAME}\",
       \"agencyName\": \"${AGENCY_NAME}\",
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
