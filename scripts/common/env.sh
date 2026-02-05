#!/bin/bash

# ==============================================================================
# [공통 환경 설정 파일]
# ==============================================================================

# Server Configuration
BASE_URL="http://localhost:8080/api/reservations"
CONTENT_TYPE="Content-Type: application/json"

# Curl Timeout 설정
CURL_OPTS="--connect-timeout 5 --max-time 10 -s"

# Test Data
# [주의] 예약이 완료된 ID는 중복 예약 시 실패하므로, 새로운 테스트 시 이 값을 변경하세요.
DEFAULT_USER_ID=1
DEFAULT_SEAT_ID=2

# Colors for Output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'