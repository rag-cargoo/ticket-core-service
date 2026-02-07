# TODO List (Ticket Core Service)

##  Next Milestone: Step 5 - Redis Sorted Set 기반 대기열 시스템

- [ ] Redis Sorted Set을 이용한 대기 순번(Rank) 발급 로직 구현
- [ ] 사용자별 실시간 대기 순번 및 예상 시간 조회 API 추가
- [ ] 대기열 진입 -> 순번 대기 -> 예약 가능 상태 전환 워크플로우 완성

##  Completed Tasks
- [x] Step 1~2: 낙관적/비관적 락 구현 및 검증
- [x] Step 3: Redis 분산 락(Redisson) Facade 구현 및 검증
- [x] Step 4: Kafka 기반 비동기 처리 및 SSE 알림 시스템 구축
- [x] Admin API 추가: 테스트 데이터 자동 셋업용 `/api/concerts/setup`
- [x] DTO 레이어 정규화: `UserRequest`, `ConcertResponse` 등 파일 분리 및 중복 제거

##  Future Milestones
- [ ] 부하 테스트(k6)를 통한 시스템 임계치 측정 및 보고서 작성
- [ ] 프론트엔드 연동 및 통합 테스트
- [ ] 아티스트/기획사 엔티티 계층 구조 확장 (Phase 1.5)
