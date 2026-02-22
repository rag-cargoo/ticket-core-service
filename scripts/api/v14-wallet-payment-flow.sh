#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
USER_API="${API_HOST}/api/users"
CONCERT_API="${API_HOST}/api/concerts"
RESERVATION_API="${API_HOST}/api/reservations"
CONTENT_TYPE="Content-Type: application/json"
TS="$(date +%s)"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

USER_ID=""
CONCERT_ID=""

cleanup() {
    if [[ -n "${USER_ID}" ]]; then
        curl -s -X DELETE "${USER_API}/${USER_ID}" >/dev/null 2>&1 || true
    fi
    if [[ -n "${CONCERT_ID}" ]]; then
        curl -s -X DELETE "${CONCERT_API}/cleanup/${CONCERT_ID}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

extract_json_number() {
    local key="$1"
    local body="$2"
    echo "${body}" | grep -oP "\"${key}\":\\s*\\K-?[0-9]+" | head -n 1 || true
}

echo -e "${BLUE}>>>> [v14 Test] Wallet + Reservation Payment/Refund 흐름 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 1] 사용자 생성... ${NC}"
USER_BODY="$(curl -s -X POST "${USER_API}" -H "${CONTENT_TYPE}" -d "{\"username\":\"wallet-user-${TS}\",\"tier\":\"BASIC\"}")"
USER_ID="$(extract_json_number "id" "${USER_BODY}")"
if [[ -z "${USER_ID}" ]]; then
    echo -e "${RED}실패! 사용자 생성 실패: ${USER_BODY}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (userId=${USER_ID})${NC}"

echo -ne "${YELLOW}[Step 2] 초기 잔액 확인... ${NC}"
WALLET_BODY="$(curl -s "${USER_API}/${USER_ID}/wallet")"
INITIAL_BALANCE="$(extract_json_number "walletBalanceAmount" "${WALLET_BODY}")"
if [[ "${INITIAL_BALANCE}" != "200000" ]]; then
    echo -e "${RED}실패! 초기 잔액이 기대값과 다름: ${WALLET_BODY}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (balance=${INITIAL_BALANCE})${NC}"

echo -ne "${YELLOW}[Step 3] 지갑 충전(50,000)... ${NC}"
CHARGE_KEY="wallet-charge-${USER_ID}-${TS}"
CHARGE_RESPONSE="$(curl -s -X POST "${USER_API}/${USER_ID}/wallet/charges" -H "${CONTENT_TYPE}" -d "{
  \"amount\": 50000,
  \"idempotencyKey\": \"${CHARGE_KEY}\",
  \"description\": \"SCRIPT_CHARGE\"
}")"
CHARGE_TYPE="$(echo "${CHARGE_RESPONSE}" | grep -oP '"type":"\K[^"]+' || true)"
if [[ "${CHARGE_TYPE}" != "CHARGE" ]]; then
    echo -e "${RED}실패! 충전 결과 오류: ${CHARGE_RESPONSE}${NC}"
    exit 1
fi
WALLET_BODY="$(curl -s "${USER_API}/${USER_ID}/wallet")"
BALANCE_AFTER_CHARGE="$(extract_json_number "walletBalanceAmount" "${WALLET_BODY}")"
if [[ "${BALANCE_AFTER_CHARGE}" != "250000" ]]; then
    echo -e "${RED}실패! 충전 후 잔액 오류: ${WALLET_BODY}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (balance=${BALANCE_AFTER_CHARGE})${NC}"

echo -ne "${YELLOW}[Step 4] 공연/좌석 셋업... ${NC}"
SETUP_BODY="$(curl -s -X POST "${CONCERT_API}/setup" -H "${CONTENT_TYPE}" -d "{
  \"title\": \"WALLET_FLOW_${TS}\",
  \"artistName\": \"WALLET_ARTIST_${TS}\",
  \"entertainmentName\": \"WALLET_AGENCY_${TS}\",
  \"concertDate\": \"2026-06-01T18:00:00\",
  \"seatCount\": 10
}")"
CONCERT_ID="$(echo "${SETUP_BODY}" | grep -oP 'ConcertID=\K[0-9]+' || true)"
OPTION_ID="$(echo "${SETUP_BODY}" | grep -oP 'OptionID=\K[0-9]+' || true)"
if [[ -z "${CONCERT_ID}" || -z "${OPTION_ID}" ]]; then
    echo -e "${RED}실패! 공연 셋업 실패: ${SETUP_BODY}${NC}"
    exit 1
fi
SEAT_ID="$(curl -s "${CONCERT_API}/options/${OPTION_ID}/seats" | grep -oP '"id":\s*\K[0-9]+' | head -n 1 || true)"
if [[ -z "${SEAT_ID}" ]]; then
    echo -e "${RED}실패! 좌석 조회 실패${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (concertId=${CONCERT_ID}, seatId=${SEAT_ID})${NC}"

echo -ne "${YELLOW}[Step 5] 예약 HOLD -> PAYING -> CONFIRMED... ${NC}"
HOLD_BODY="$(curl -s -X POST "${RESERVATION_API}/v6/holds" -H "${CONTENT_TYPE}" -d "{
  \"userId\": ${USER_ID},
  \"seatId\": ${SEAT_ID},
  \"requestFingerprint\": \"wallet-flow-hold-${TS}\",
  \"deviceFingerprint\": \"wallet-flow-device-${TS}\"
}")"
RESERVATION_ID="$(extract_json_number "id" "${HOLD_BODY}")"
if [[ -z "${RESERVATION_ID}" ]]; then
    echo -e "${RED}실패! HOLD 생성 실패: ${HOLD_BODY}${NC}"
    exit 1
fi
curl -s -X POST "${RESERVATION_API}/v6/${RESERVATION_ID}/paying" -H "${CONTENT_TYPE}" -d "{\"userId\": ${USER_ID}}" >/dev/null
CONFIRM_BODY="$(curl -s -X POST "${RESERVATION_API}/v6/${RESERVATION_ID}/confirm" -H "${CONTENT_TYPE}" -d "{\"userId\": ${USER_ID}}")"
CONFIRM_STATUS="$(echo "${CONFIRM_BODY}" | grep -oP '"status":"\K[^"]+' || true)"
if [[ "${CONFIRM_STATUS}" != "CONFIRMED" ]]; then
    echo -e "${RED}실패! CONFIRM 실패: ${CONFIRM_BODY}${NC}"
    exit 1
fi
WALLET_BODY="$(curl -s "${USER_API}/${USER_ID}/wallet")"
BALANCE_AFTER_CONFIRM="$(extract_json_number "walletBalanceAmount" "${WALLET_BODY}")"
if [[ "${BALANCE_AFTER_CONFIRM}" != "150000" ]]; then
    echo -e "${RED}실패! 결제 차감 잔액 오류: ${WALLET_BODY}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (balance=${BALANCE_AFTER_CONFIRM})${NC}"

echo -ne "${YELLOW}[Step 6] CANCEL -> REFUND... ${NC}"
curl -s -X POST "${RESERVATION_API}/v6/${RESERVATION_ID}/cancel" -H "${CONTENT_TYPE}" -d "{\"userId\": ${USER_ID}}" >/dev/null
REFUND_BODY="$(curl -s -X POST "${RESERVATION_API}/v6/${RESERVATION_ID}/refund" -H "${CONTENT_TYPE}" -d "{\"userId\": ${USER_ID}}")"
REFUND_STATUS="$(echo "${REFUND_BODY}" | grep -oP '"status":"\K[^"]+' || true)"
if [[ "${REFUND_STATUS}" != "REFUNDED" ]]; then
    echo -e "${RED}실패! REFUND 실패: ${REFUND_BODY}${NC}"
    exit 1
fi
WALLET_BODY="$(curl -s "${USER_API}/${USER_ID}/wallet")"
BALANCE_AFTER_REFUND="$(extract_json_number "walletBalanceAmount" "${WALLET_BODY}")"
if [[ "${BALANCE_AFTER_REFUND}" != "250000" ]]; then
    echo -e "${RED}실패! 환불 후 잔액 오류: ${WALLET_BODY}${NC}"
    exit 1
fi
echo -e "${GREEN}성공 (balance=${BALANCE_AFTER_REFUND})${NC}"

echo -ne "${YELLOW}[Step 7] 거래 원장 조회... ${NC}"
LEDGER_BODY="$(curl -s "${USER_API}/${USER_ID}/wallet/transactions?limit=20")"
if [[ "${LEDGER_BODY}" != *"\"type\":\"PAYMENT\""* || "${LEDGER_BODY}" != *"\"type\":\"REFUND\""* ]]; then
    echo -e "${RED}실패! 원장에 PAYMENT/REFUND가 보이지 않음: ${LEDGER_BODY}${NC}"
    exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -e "${GREEN}>>>> [v14 Test] 검증 종료 (PASS).${NC}"
