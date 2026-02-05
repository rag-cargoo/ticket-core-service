#!/bin/bash

# ==============================================================================
# [공통 환경 설정 파일]
# 이 파일은 모든 테스트 스크립트(scripts/api/*.sh)에서 공유하는 변수들을 정의합니다.
#
# 수정 및 확인 필요 항목:
# 1. BASE_URL: 서버 주소 및 포트가 다를 경우 수정 (기본: localhost:8080)
# 2. DEFAULT_USER_ID: 테스트에 사용할 유저의 ID
# 3. DEFAULT_SEAT_ID: 테스트에 사용할 좌석의 ID (DB에 존재하는 ID여야 함)
# ==============================================================================

# Server Configuration
BASE_URL="http://localhost:8080/api"
CONTENT_TYPE="Content-Type: application/json"

# Test Data (상황에 맞게 이 값을 변경하여 테스트하세요)
DEFAULT_USER_ID=1
DEFAULT_SEAT_ID=1

# Colors for Output (터미널 가독성용)
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color