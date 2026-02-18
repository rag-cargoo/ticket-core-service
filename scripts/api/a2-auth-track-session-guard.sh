#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}>>>> [a2-auth Test] Auth Track A2 인증 세션/가드 계약 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 1] /api/auth/me 무토큰 접근 차단 확인... ${NC}"
ME_RESPONSE=$(curl ${CURL_OPTS} "${API_HOST}/api/auth/me")
ME_BODY=$(echo "${ME_RESPONSE}" | sed '$d')
ME_CODE=$(echo "${ME_RESPONSE}" | tail -n1)
if [[ "${ME_CODE}" != "401" ]] || ! echo "${ME_BODY}" | grep -q '"message":"unauthorized"' || ! echo "${ME_BODY}" | grep -q '"errorCode":"AUTH_ACCESS_TOKEN_REQUIRED"'; then
  echo -e "${RED}실패 (code=${ME_CODE}, body=${ME_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 2] refresh token 필수 검증 확인... ${NC}"
REFRESH_RESPONSE=$(curl ${CURL_OPTS} -X POST "${API_HOST}/api/auth/token/refresh" \
  -H "Content-Type: application/json" \
  -d '{}')
REFRESH_BODY=$(echo "${REFRESH_RESPONSE}" | sed '$d')
REFRESH_CODE=$(echo "${REFRESH_RESPONSE}" | tail -n1)
if [[ "${REFRESH_CODE}" != "400" ]] || ! echo "${REFRESH_BODY}" | grep -q '"message":"refresh token is required"' || ! echo "${REFRESH_BODY}" | grep -q '"errorCode":"AUTH_REFRESH_TOKEN_REQUIRED"'; then
  echo -e "${RED}실패 (code=${REFRESH_CODE}, body=${REFRESH_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 3] v7 HOLD 무토큰 접근 차단 확인... ${NC}"
HOLD_RESPONSE=$(curl ${CURL_OPTS} -X POST "${API_HOST}/api/reservations/v7/holds" \
  -H "Content-Type: application/json" \
  -d '{"seatId":1}')
HOLD_BODY=$(echo "${HOLD_RESPONSE}" | sed '$d')
HOLD_CODE=$(echo "${HOLD_RESPONSE}" | tail -n1)
if [[ "${HOLD_CODE}" != "401" ]] || ! echo "${HOLD_BODY}" | grep -q '"message":"unauthorized"' || ! echo "${HOLD_BODY}" | grep -q '"errorCode":"AUTH_ACCESS_TOKEN_REQUIRED"'; then
  echo -e "${RED}실패 (code=${HOLD_CODE}, body=${HOLD_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -e "${GREEN}>>>> [a2-auth Test] 검증 종료 (PASS).${NC}"
