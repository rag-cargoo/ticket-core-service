#!/bin/bash
# ==============================================================================
# [Step 5 Test] Redis Sorted Set 기반 대기열 기능 검증
# ==============================================================================
set -e # 오류 발생 시 즉시 종료
set -u # 미정의 변수 사용 시 종료

BASE_URL="http://localhost:8080/api/v1/waiting-queue"
CONCERT_ID=1

echo ">>>> 🎫 [Step 5] 실시간 대기열 진입 및 순번 테스트 시작..."
echo "------------------------------------------------------------"

# 1. 5명의 유저 순차 진입
for i in {1..5}
do
    USER_ID=$((100 + i))
    echo "  [User $USER_ID] 대기열 진입 시도..."
    
    # 가독성을 위해 한 줄로 작성하여 구문 오류 방지
    curl -s -X POST "$BASE_URL/join" -H "Content-Type: application/json" -d "{\"userId\": $USER_ID, \"concertId\": $CONCERT_ID}" | jq -c '.'
done

echo "------------------------------------------------------------"
echo ">>>> 🔍 현재 전체 대기 상태 확인 (Polling)"
echo "------------------------------------------------------------"

for i in {1..5}
do
    USER_ID=$((100 + i))
    echo -n "  [User $USER_ID] 상태 조회: "
    curl -s "$BASE_URL/status?userId=$USER_ID&concertId=$CONCERT_ID" | jq -c '.'
done

echo "------------------------------------------------------------"
echo ">>>> [SUCCESS] 검증 완료!"