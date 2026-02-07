# TODO List (Ticket Core Service)

##  Current Milestone: Step 7 - SSE 기반 실시간 순번 자동 푸시

- [ ] 대기열 SSE 구독 엔드포인트 추가 (`/api/v1/waiting-queue` 계열)
- [ ] 대기 순번 변경 이벤트(`RANK_UPDATE`) 자동 푸시 흐름 완성
- [ ] 활성 전환 시점(`ACTIVE`) 알림 페이로드 표준화
- [ ] 타임아웃/재연결 포함 SSE 연결 수명주기 안정화
- [ ] Step 7 API 명세 및 테스트 스크립트 추가

##  Completed Tasks
- [x] Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
- [x] Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
- [x] Step 3: Redis 분산 락(Redisson) 구현 및 검증
- [x] Step 4: Kafka 기반 비동기 예약 처리 + SSE 결과 알림 구현
- [x] Step 5: Redis Sorted Set 기반 대기열 순번 조회 구현
- [x] Step 6: Throttling + 인터셉터 기반 대기열 진입 제어 구현

##  Backlog
- [ ] 부하 테스트(k6)를 통한 임계치 측정 및 보고서 작성
- [ ] 프론트엔드 연동 및 통합 시나리오 검증
- [ ] 공연 조회 캐싱 전략 도입
- [ ] 아티스트/기획사 엔티티 확장 (Phase 1.5)
