#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
CONCERT_API="${CONCERT_API:-${API_HOST}/api/concerts}"
RESERVATION_API="${RESERVATION_API:-${API_HOST}/api/reservations/v6}"
USER_API="${USER_API:-${API_HOST}/api/users}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

CONCERT_TITLE="v15_sale_status_$(date +%s)"
ARTIST_NAME="v15_artist_$(date +%s)"
AGENCY_NAME="v15_agency_$(date +%s)"
CONCERT_DATE="$(date -d '+10 days' '+%Y-%m-%dT%H:%M:%S')"

extract_item_field() {
  local json_payload="$1"
  local field="$2"
  JSON_PAYLOAD="$json_payload" python3 - "$field" <<'PY'
import json
import os
import sys

field = sys.argv[1]
data = json.loads(os.environ["JSON_PAYLOAD"])
items = data.get("items") or []
if not items:
    print("")
    sys.exit(0)
value = items[0].get(field)
if isinstance(value, bool):
    print("true" if value else "false")
elif value is None:
    print("")
else:
    print(value)
PY
}

search_target_concert() {
  local search_json
  search_json="$(curl -s "${CONCERT_API}/search?keyword=${CONCERT_TITLE}&page=0&size=1&sort=id,desc")"
  if [[ "${search_json}" != *"\"errorCode\""* ]]; then
    echo "${search_json}"
    return 0
  fi

  # Fallback: 일부 런타임(PostgreSQL)에서 nullable search 파라미터 타입 추론 이슈가 있을 때
  # 목록 API에서 title로 직접 필터링해 계약 검증을 계속 진행한다.
  local list_json
  list_json="$(curl -s "${CONCERT_API}")"
  JSON_PAYLOAD="${list_json}" TARGET_TITLE="${CONCERT_TITLE}" python3 - <<'PY'
import json
import os

target = os.environ["TARGET_TITLE"]
rows = json.loads(os.environ["JSON_PAYLOAD"])
if not isinstance(rows, list):
    print('{"items":[]}')
    raise SystemExit(0)
matches = [row for row in rows if row.get("title") == target]
if not matches:
    print('{"items":[]}')
    raise SystemExit(0)
matches.sort(key=lambda row: row.get("id", 0), reverse=True)
print(json.dumps({"items": [matches[0]]}, ensure_ascii=False))
PY
}

assert_equals() {
  local actual="$1"
  local expected="$2"
  local message="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo -e "${RED}실패: ${message} (actual=${actual}, expected=${expected})${NC}"
    exit 1
  fi
}

assert_number_range() {
  local value="$1"
  local min="$2"
  local max="$3"
  local message="$4"
  if [[ ! "$value" =~ ^[0-9]+$ ]]; then
    echo -e "${RED}실패: ${message} (not number: ${value})${NC}"
    exit 1
  fi
  if (( value < min || value > max )); then
    echo -e "${RED}실패: ${message} (value=${value}, range=${min}..${max})${NC}"
    exit 1
  fi
}

set_sales_policy() {
  local concert_id="$1"
  local general_sale_start_at="$2"
  local response
  local body
  local code

  response=$(curl ${CURL_OPTS} -X PUT "${CONCERT_API}/${concert_id}/sales-policy" \
    -H "Content-Type: application/json" \
    -d "{
      \"presaleStartAt\":\"2000-01-01T00:00:00\",
      \"presaleEndAt\":\"2000-01-02T00:00:00\",
      \"presaleMinimumTier\":\"BASIC\",
      \"generalSaleStartAt\":\"${general_sale_start_at}\",
      \"maxReservationsPerUser\":10
    }")
  body=$(echo "${response}" | sed '$d')
  code=$(echo "${response}" | tail -n 1)
  if [[ "$code" != "200" ]]; then
    echo -e "${RED}실패: 판매정책 설정 실패 (code=${code}, body=${body})${NC}"
    exit 1
  fi
}

cleanup() {
  if [[ -n "${TEST_USER_ID:-}" ]]; then
    curl -s -X DELETE "${USER_API}/${TEST_USER_ID}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${CONCERT_ID:-}" ]]; then
    curl -s -X DELETE "${CONCERT_API}/cleanup/${CONCERT_ID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo -e "${BLUE}>>>> [v15 Test] 콘서트 판매상태/카운트다운 계약 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 1] 테스트 공연 셋업... ${NC}"
SETUP_RESPONSE=$(curl -s -X POST "${CONCERT_API}/setup" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"${CONCERT_TITLE}\",
    \"artistName\":\"${ARTIST_NAME}\",
    \"agencyName\":\"${AGENCY_NAME}\",
    \"concertDate\":\"${CONCERT_DATE}\",
    \"seatCount\":1
  }")
CONCERT_ID=$(echo "${SETUP_RESPONSE}" | grep -oP 'ConcertID=\K\d+' || true)
OPTION_ID=$(echo "${SETUP_RESPONSE}" | grep -oP 'OptionID=\K\d+' || true)
if [[ -z "${CONCERT_ID}" || -z "${OPTION_ID}" ]]; then
  echo -e "${RED}실패 (setup response=${SETUP_RESPONSE})${NC}"
  exit 1
fi
echo -e "${GREEN}성공 (concertId=${CONCERT_ID}, optionId=${OPTION_ID})${NC}"

echo -ne "${YELLOW}[Step 2] 정책 미설정 상태(UNSCHEDULED) 검증... ${NC}"
SEARCH_JSON=$(search_target_concert)
SALE_STATUS=$(extract_item_field "${SEARCH_JSON}" "saleStatus")
VISIBLE=$(extract_item_field "${SEARCH_JSON}" "reservationButtonVisible")
ENABLED=$(extract_item_field "${SEARCH_JSON}" "reservationButtonEnabled")
TOTAL_COUNT=$(extract_item_field "${SEARCH_JSON}" "totalSeatCount")
AVAILABLE_COUNT=$(extract_item_field "${SEARCH_JSON}" "availableSeatCount")
assert_equals "${SALE_STATUS}" "UNSCHEDULED" "saleStatus"
assert_equals "${VISIBLE}" "false" "reservationButtonVisible"
assert_equals "${ENABLED}" "false" "reservationButtonEnabled"
assert_equals "${TOTAL_COUNT}" "1" "totalSeatCount"
assert_equals "${AVAILABLE_COUNT}" "1" "availableSeatCount"
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 3] PREOPEN(1시간 초과) 상태 검증... ${NC}"
PREOPEN_AT="$(date -d '+2 hours' '+%Y-%m-%dT%H:%M:%S')"
set_sales_policy "${CONCERT_ID}" "${PREOPEN_AT}"
SEARCH_JSON=$(search_target_concert)
SALE_STATUS=$(extract_item_field "${SEARCH_JSON}" "saleStatus")
VISIBLE=$(extract_item_field "${SEARCH_JSON}" "reservationButtonVisible")
ENABLED=$(extract_item_field "${SEARCH_JSON}" "reservationButtonEnabled")
OPENS_IN=$(extract_item_field "${SEARCH_JSON}" "saleOpensInSeconds")
assert_equals "${SALE_STATUS}" "PREOPEN" "saleStatus"
assert_equals "${VISIBLE}" "false" "reservationButtonVisible"
assert_equals "${ENABLED}" "false" "reservationButtonEnabled"
assert_number_range "${OPENS_IN}" 3601 10800 "saleOpensInSeconds(PREOPEN)"
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 4] OPEN_SOON_1H 상태 검증... ${NC}"
OPEN_SOON_1H_AT="$(date -d '+40 minutes' '+%Y-%m-%dT%H:%M:%S')"
set_sales_policy "${CONCERT_ID}" "${OPEN_SOON_1H_AT}"
SEARCH_JSON=$(search_target_concert)
SALE_STATUS=$(extract_item_field "${SEARCH_JSON}" "saleStatus")
VISIBLE=$(extract_item_field "${SEARCH_JSON}" "reservationButtonVisible")
ENABLED=$(extract_item_field "${SEARCH_JSON}" "reservationButtonEnabled")
OPENS_IN=$(extract_item_field "${SEARCH_JSON}" "saleOpensInSeconds")
assert_equals "${SALE_STATUS}" "OPEN_SOON_1H" "saleStatus"
assert_equals "${VISIBLE}" "true" "reservationButtonVisible"
assert_equals "${ENABLED}" "false" "reservationButtonEnabled"
assert_number_range "${OPENS_IN}" 301 3600 "saleOpensInSeconds(OPEN_SOON_1H)"
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 5] OPEN_SOON_5M 상태 검증... ${NC}"
OPEN_SOON_5M_AT="$(date -d '+3 minutes' '+%Y-%m-%dT%H:%M:%S')"
set_sales_policy "${CONCERT_ID}" "${OPEN_SOON_5M_AT}"
SEARCH_JSON=$(search_target_concert)
SALE_STATUS=$(extract_item_field "${SEARCH_JSON}" "saleStatus")
VISIBLE=$(extract_item_field "${SEARCH_JSON}" "reservationButtonVisible")
ENABLED=$(extract_item_field "${SEARCH_JSON}" "reservationButtonEnabled")
OPENS_IN=$(extract_item_field "${SEARCH_JSON}" "saleOpensInSeconds")
assert_equals "${SALE_STATUS}" "OPEN_SOON_5M" "saleStatus"
assert_equals "${VISIBLE}" "true" "reservationButtonVisible"
assert_equals "${ENABLED}" "false" "reservationButtonEnabled"
assert_number_range "${OPENS_IN}" 1 300 "saleOpensInSeconds(OPEN_SOON_5M)"
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 6] OPEN 상태 검증... ${NC}"
OPEN_AT="$(date -d '-1 minute' '+%Y-%m-%dT%H:%M:%S')"
set_sales_policy "${CONCERT_ID}" "${OPEN_AT}"
SEARCH_JSON=$(search_target_concert)
SALE_STATUS=$(extract_item_field "${SEARCH_JSON}" "saleStatus")
VISIBLE=$(extract_item_field "${SEARCH_JSON}" "reservationButtonVisible")
ENABLED=$(extract_item_field "${SEARCH_JSON}" "reservationButtonEnabled")
OPENS_IN=$(extract_item_field "${SEARCH_JSON}" "saleOpensInSeconds")
assert_equals "${SALE_STATUS}" "OPEN" "saleStatus"
assert_equals "${VISIBLE}" "true" "reservationButtonVisible"
assert_equals "${ENABLED}" "true" "reservationButtonEnabled"
assert_equals "${OPENS_IN}" "0" "saleOpensInSeconds(OPEN)"
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 7] SOLD_OUT 상태 검증... ${NC}"
TEST_USER_ID=$(curl -s -X POST "${USER_API}" -H "Content-Type: application/json" \
  -d "{\"username\":\"test_v15_$(date +%s)\"}" | grep -oP '"id":\s*\K\d+' || true)
if [[ -z "${TEST_USER_ID}" ]]; then
  echo -e "${RED}실패 (test user 생성 실패)${NC}"
  exit 1
fi
SEAT_ID=$(curl -s "${CONCERT_API}/options/${OPTION_ID}/seats" | grep -oP '"id":\s*\K\d+' | head -n 1 || true)
if [[ -z "${SEAT_ID}" ]]; then
  echo -e "${RED}실패 (seat 조회 실패)${NC}"
  exit 1
fi
HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${RESERVATION_API}/holds" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${TEST_USER_ID},\"seatId\":${SEAT_ID}}")
HOLD_BODY=$(echo "${HOLD_RESPONSE}" | sed '$d')
HOLD_CODE=$(echo "${HOLD_RESPONSE}" | tail -n 1)
if [[ "${HOLD_CODE}" != "201" ]]; then
  echo -e "${RED}실패 (hold code=${HOLD_CODE}, body=${HOLD_BODY})${NC}"
  exit 1
fi
SEARCH_JSON=$(search_target_concert)
SALE_STATUS=$(extract_item_field "${SEARCH_JSON}" "saleStatus")
VISIBLE=$(extract_item_field "${SEARCH_JSON}" "reservationButtonVisible")
ENABLED=$(extract_item_field "${SEARCH_JSON}" "reservationButtonEnabled")
AVAILABLE_COUNT=$(extract_item_field "${SEARCH_JSON}" "availableSeatCount")
TOTAL_COUNT=$(extract_item_field "${SEARCH_JSON}" "totalSeatCount")
assert_equals "${SALE_STATUS}" "SOLD_OUT" "saleStatus"
assert_equals "${VISIBLE}" "true" "reservationButtonVisible"
assert_equals "${ENABLED}" "false" "reservationButtonEnabled"
assert_equals "${AVAILABLE_COUNT}" "0" "availableSeatCount"
assert_equals "${TOTAL_COUNT}" "1" "totalSeatCount"
echo -e "${GREEN}성공${NC}"

echo -e "${GREEN}>>>> [v15 Test] 검증 종료 (PASS).${NC}"
