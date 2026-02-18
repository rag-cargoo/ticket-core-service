# Makefile for TicketRush API Testing

.PHONY: help test-v1 test-v2 test-v3 test-v4 test-v5 test-v6 test-v7 test-v9 test-k6 test-k6-dashboard test-suite test-auth-social-pipeline test-auth-social-real-provider test-all setup-perms

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
	@echo " make test-v7    : [v7] SSE 순번 자동 푸시 테스트"
	@echo " make test-v9    : [v9] Step10 취소/환불/재판매 연계 테스트"
	@echo " make test-k6    : [perf] k6 대기열 부하 테스트"
	@echo " make test-k6-dashboard : [perf] k6 + 웹 대시보드(5665) 실행"
	@echo " make test-suite : 변경된 API 스크립트 실행 + 리포트 생성"
	@echo " make test-auth-social-pipeline : auth-social CI-safe 파이프라인 테스트"
	@echo " make test-auth-social-real-provider : auth-social real provider E2E (선택 실행)"
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

test-v7:
	/bin/bash ./scripts/api/v7-sse-rank-push.sh

test-v9:
	/bin/bash ./scripts/api/v9-cancel-refund-resale.sh

test-k6:
	bash ./scripts/perf/run-k6-waiting-queue.sh

test-k6-dashboard:
	K6_WEB_DASHBOARD=true K6_DURATION=$${K6_DURATION:-30s} bash ./scripts/perf/run-k6-waiting-queue.sh

test-suite:
	bash ./scripts/api/run-api-script-tests.sh

test-auth-social-pipeline:
	bash ./scripts/api/run-auth-social-e2e-pipeline.sh

test-auth-social-real-provider:
	bash ./scripts/api/run-auth-social-real-provider-e2e.sh

# 실행 권한 부여
setup:
	chmod +x scripts/api/*.sh
	@echo "All scripts are now executable."
# 전체 테스트 실행
test-all: test-v1 test-v2 test-v3 test-v4 test-v5 test-v6 test-v7 test-v9
