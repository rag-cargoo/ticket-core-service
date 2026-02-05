# Makefile for TicketRush API Testing

.PHONY: help test-v1 test-v2 test-v3 test-all setup-perms

# 기본 명령어 (도움말)
help:
	@echo "========================================================================"
	@echo " TicketRush API Test Automation"
	@echo "========================================================================"
	@echo " make test-v1   : [v1] 낙관적 락 예약 API 테스트"
	@echo " make test-v2   : [v2] 비관적 락 예약 API 테스트"
	@echo " make test-v3   : [v3] Redis 분산 락 예약 API 테스트"
	@echo " make test-all  : 모든 버전 순차 테스트"
	@echo " make setup     : 스크립트 실행 권한 부여"
	@echo "========================================================================"

# 실행 권한 부여
setup:
	chmod +x scripts/api/*.sh
	chmod +x scripts/bench/*.sh

# 개별 테스트 실행
test-v1:
	./scripts/api/v1-optimistic.sh

test-v2:
	./scripts/api/v2-pessimistic.sh

test-v3:
	./scripts/api/v3-distributed.sh

# 전체 테스트 실행
test-all: test-v1 test-v2 test-v3
