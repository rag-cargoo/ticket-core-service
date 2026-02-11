#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
BASE_URL="${API_HOST}/api/auth/social"
CURL_OPTS="-s -w \n%{http_code} --connect-timeout 5 --max-time 10"

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}>>>> [v12 Test] Auth Track 소셜 로그인 계약 검증 시작...${NC}"

echo -ne "${YELLOW}[Step 1] 카카오 authorize-url 조회... ${NC}"
KAKAO_RESPONSE=$(curl ${CURL_OPTS} "${BASE_URL}/kakao/authorize-url")
KAKAO_BODY=$(echo "${KAKAO_RESPONSE}" | sed '$d')
KAKAO_CODE=$(echo "${KAKAO_RESPONSE}" | tail -n1)
if [[ "${KAKAO_CODE}" != "200" ]] || ! echo "${KAKAO_BODY}" | grep -q '"provider":"kakao"'; then
  echo -e "${RED}실패 (code=${KAKAO_CODE}, body=${KAKAO_BODY})${NC}"
  exit 1
fi
if ! echo "${KAKAO_BODY}" | grep -q 'kauth.kakao.com/oauth/authorize'; then
  echo -e "${RED}실패 (kakao authorize endpoint 누락)${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 2] 네이버 authorize-url 조회... ${NC}"
NAVER_RESPONSE=$(curl ${CURL_OPTS} "${BASE_URL}/naver/authorize-url")
NAVER_BODY=$(echo "${NAVER_RESPONSE}" | sed '$d')
NAVER_CODE=$(echo "${NAVER_RESPONSE}" | tail -n1)
if [[ "${NAVER_CODE}" != "200" ]] || ! echo "${NAVER_BODY}" | grep -q '"provider":"naver"'; then
  echo -e "${RED}실패 (code=${NAVER_CODE}, body=${NAVER_BODY})${NC}"
  exit 1
fi
if ! echo "${NAVER_BODY}" | grep -q 'nid.naver.com/oauth2.0/authorize'; then
  echo -e "${RED}실패 (naver authorize endpoint 누락)${NC}"
  exit 1
fi
if ! echo "${NAVER_BODY}" | grep -Eq '"state":"[^"]+"'; then
  echo -e "${RED}실패 (state 미생성)${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 3] 카카오 exchange 파라미터 검증... ${NC}"
BAD_KAKAO_RESPONSE=$(curl ${CURL_OPTS} -X POST "${BASE_URL}/kakao/exchange" \
  -H "Content-Type: application/json" \
  -d '{}')
BAD_KAKAO_BODY=$(echo "${BAD_KAKAO_RESPONSE}" | sed '$d')
BAD_KAKAO_CODE=$(echo "${BAD_KAKAO_RESPONSE}" | tail -n1)
if [[ "${BAD_KAKAO_CODE}" != "400" ]] || ! echo "${BAD_KAKAO_BODY}" | grep -q 'authorization code is required'; then
  echo -e "${RED}실패 (code=${BAD_KAKAO_CODE}, body=${BAD_KAKAO_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -ne "${YELLOW}[Step 4] 네이버 exchange state 검증... ${NC}"
BAD_NAVER_RESPONSE=$(curl ${CURL_OPTS} -X POST "${BASE_URL}/naver/exchange" \
  -H "Content-Type: application/json" \
  -d '{"code":"dummy-code"}')
BAD_NAVER_BODY=$(echo "${BAD_NAVER_RESPONSE}" | sed '$d')
BAD_NAVER_CODE=$(echo "${BAD_NAVER_RESPONSE}" | tail -n1)
if [[ "${BAD_NAVER_CODE}" != "400" ]] || ! echo "${BAD_NAVER_BODY}" | grep -q 'state is required'; then
  echo -e "${RED}실패 (code=${BAD_NAVER_CODE}, body=${BAD_NAVER_BODY})${NC}"
  exit 1
fi
echo -e "${GREEN}성공${NC}"

echo -e "${GREEN}>>>> [v12 Test] 검증 종료 (PASS).${NC}"
