#!/bin/bash

# ==============================================================================
# [Admin] 테스트 환경 자동 구축 스크립트
# 신규 공연, 옵션, 좌석을 API를 통해 생성합니다.
# ==============================================================================

source "$(dirname "$0")/../common/env.sh"

CONCERT_TITLE="TEST_CONCERT_$(date +%s)"
CONCERT_DATE=$(date -d "+10 days" +"%Y-%m-%dT%H:%M:%S")

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[Admin] Setting up new test environment...${NC}"
echo -e "${BLUE}====================================================${NC}"

# 역슬래시(\)를 사용하여 명령어를 연결합니다.
RESPONSE=$(curl -v -s -X POST "http://127.0.0.1:8080/api/concerts/setup" \
     -H "${CONTENT_TYPE}" \
     -d "{
       \"title\": \"${CONCERT_TITLE}\",
       \"artistName\": \"NewJeans\",
       \"agencyName\": \"ADOR\",
       \"concertDate\": \"${CONCERT_DATE}\",
       \"seatCount\": 50
     }")

echo -e "Full Response: ${RESPONSE}"

# 생성된 ID 추출
CONCERT_ID=$(echo $RESPONSE | grep -oP 'ConcertID=\K\d+')
OPTION_ID=$(echo $RESPONSE | grep -oP 'OptionID=\K\d+')

if [ -z "$OPTION_ID" ]; then
    echo -e "${RED}Failed to setup environment.${NC}"
    exit 1
fi

# 1번 좌석의 ID 조회
echo -e "\n${YELLOW}[Step 2] Fetching the first available seat ID...${NC}"
SEAT_ID=$(curl -s "http://127.0.0.1:8080/api/concerts/options/${OPTION_ID}/seats" \
    | grep -oP '"id":\s*\K\d+' | head -n 1)

if [ -z "$SEAT_ID" ]; then
    echo -e "${RED}Failed to fetch Seat ID.${NC}"
    exit 1
fi

echo -e "${GREEN}Ready for testing! SEAT_ID=${SEAT_ID}${NC}"
echo -e "${BLUE}====================================================${NC}"

# 환경 변수 파일에 저장 (다른 스크립트에서 참조 가능하도록)
echo "export LATEST_SEAT_ID=${SEAT_ID}" > "$(dirname "$0")/../common/last_setup.sh"
chmod +x "$(dirname "$0")/../common/last_setup.sh"