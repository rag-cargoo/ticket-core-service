# TODO List (Ticket Core Service)

> Step 6까지 완료된 상태에서, Step 7 구현 항목과 후속 백로그를 관리합니다.

---

## Current Milestone: Step 7 - SSE 기반 실시간 순번 자동 푸시
---
> [!NOTE]
> 대기열 API/스케줄러/SSE 발행 흐름을 연결하는 구간입니다.
>
> - [x] 대기열 SSE 구독 엔드포인트 추가 (`/api/v1/waiting-queue/subscribe`)
> - [x] 대기 순번 변경 이벤트(`RANK_UPDATE`) 자동 푸시 흐름 완성
> - [x] 활성 전환 시점(`ACTIVE`) 알림 페이로드 표준화
> - [x] 타임아웃/재연결 포함 SSE 연결 수명주기 안정화
> - [x] API 변경 시 문서/HTTP/API스크립트 자동 체인 검증(pre-commit quick/strict) 구성
> - [x] API 스크립트 실행 결과 리포트 자동 생성 (`prj-docs/api-test/latest.md`)
> - [ ] Step 7 API 명세/HTTP 파일/스크립트 최종 동기화 및 회귀 검증

---

## Completed Tasks
---
> [!TIP]
> 아래 항목은 코드/문서 기준으로 완료가 확인된 작업입니다.
>
> - [x] Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
> - [x] Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
> - [x] Step 3: Redis 분산 락(Redisson) 구현 및 검증
> - [x] Step 4: Kafka 기반 비동기 예약 처리 + SSE 결과 알림 구현
> - [x] Step 5: Redis Sorted Set 기반 대기열 순번 조회 구현
> - [x] Step 6: Throttling + 인터셉터 기반 대기열 진입 제어 구현

---

## Backlog
---
> [!WARNING]
> 트래픽/운영 안정성 향상과 연동 검증을 위한 후순위 작업입니다.
>
> - [ ] 부하 테스트(k6)를 통한 임계치 측정 및 보고서 작성
> - [ ] 프론트엔드 연동 및 통합 시나리오 검증
> - [ ] 공연 조회 캐싱 전략 도입
> - [ ] 아티스트/기획사 엔티티 확장 (Phase 1.5)
