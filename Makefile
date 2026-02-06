# Makefile for TicketRush API Testing

.PHONY: help test-v1 test-v2 test-v3 test-all setup-perms

# 기본 명령어 (도움말)
help:
	@echo "========================================================================"
	@echo " TicketRush API Test Automation"
	@echo "========================================================================"
	@echo " make setup-data : 테스트 기초 데이터 생성 (Concert, Seat)"
	@echo " make test-v1    : [v1] 낙관적 락 예약 API 테스트"
	@echo " make test-v2    : [v2] 비관적 락 예약 API 테스트"
	@echo " make test-v3    : [v3] Redis 분산 락 예약 API 테스트"
	@echo " make test-v6    : [v6] 유입량 제어(Throttling) 테스트"
	@echo " make test-all   : 모든 버전 순차 테스트"
	@echo " make setup      : 스크립트 실행 권한 부여"
	@echo "========================================================================"

# 데이터 초기화
setup-data:
	./scripts/api/setup-test-data.sh

# 개별 테스트 실행
test-v1:
	./scripts/api/v1-optimistic.sh

test-v2:
	./scripts/api/v2-pessimistic.sh

test-v3:
	./scripts/api/v3-distributed.sh

test-v4:
	./scripts/api/v4-polling-test.sh

test-v5:
	./scripts/api/v5-waiting-queue.sh

test-v6:
	/bin/bash ./scripts/api/v6-throttling-test.sh
# 실행 권한 부여
setup:
	chmod +x scripts/api/*.sh
	@echo "All scripts are now executable."
# 전체 테스트 실행
test-all: test-v1 test-v2 test-v3 test-v4 test-v5 test-v6
