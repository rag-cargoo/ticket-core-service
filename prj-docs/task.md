# Project Task Dashboard - Ticket Core Service

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-12 02:15:34`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 단계 목차 (Step Index)
---
> [!TIP]
> - Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
> - Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
> - Step 3: Redis 분산 락(Redisson) 구현 및 검증
> - Step 4: Kafka 기반 비동기 대기열(Waiting Queue) 구현 및 검증
> - Step 5: Redis Sorted Set 기반 실시간 대기 순번 시스템 구현
> - Step 6: 대기열 진입 제한(Throttling) 및 유입량 제어 전략 구현
> - Step 7: SSE 기반 실시간 순번 자동 푸시 시스템 구현 및 회귀 검증
> - Step 8: k6 성능 기준선 확정 및 병목 제거
> - Step 9: 결제/좌석 점유 상태머신(홀드/확정/만료) 구현
> - Step 10: 취소/환불/재판매 대기열 연계 구현
> - Step 11: 판매 정책 엔진(선예매/등급/1인 제한) 구현
> - Step 12: 부정사용 방지/감사 추적 기능 구현
> - Step 13: 인증/인가 + 소셜 로그인(카카오) 통합
> - Step 14: 프론트엔드 연동 + 검색/탐색 UX 구현
> - Step 15: 결제 샌드박스(무과금) + 웹훅 시뮬레이션 검증
> - Step 0 (락 없음): Race Condition 발생 확인 (30명 중 10명 중복 예약)
<!-- DOC_TOC_END -->

> 시스템의 현재 상태와 단계별 목표, 세부 완료 내역을 추적하는 통합 보드입니다.

---

## 현재 상태 (Status)
---
> [!NOTE]
>   - **현재 단계**: Step 10 완료 (취소/환불/재판매 대기열 연계 반영)
>   - **목표**: 고성능 선착순 티켓팅 시스템 구현
>   - **Tech Stack**: Java 17 / Spring Boot 3.4.1 / JPA / Redisson / PostgreSQL / Redis / Kafka / SSE
>   - **검증 체인**: pre-commit `quick`(기본) / `strict`(중요 커밋) 모드 운영, strict에서 문서/HTTP/API스크립트 동기화 + 실행 리포트 강제 검증

---

## 개발 원칙 (Dev Principles)
---
> [!TIP]
>   - **기술 비교/검증**: API 버전을 분리하여 관리 (v1~v6).
>   - **성능 측정**: 각 단계별 부하 테스트 결과를 기록하여 의사결정 근거로 활용.
>   - **문서화 필수**: 실험 결과와 의사결정 과정은 prj-docs/knowledge/에 상세히 기록.
>   - **안전 우선**: 파일 수정 전 원본 확인 및 파괴적 변경 시 사용자 보고 의무화.

---

## 당면 과제 (Current Tasks)
---
> [!NOTE]
>   - [x] Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
>   - [x] Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
>   - [x] Step 3: Redis 분산 락(Redisson) 구현 및 검증
>   - [x] Step 4: Kafka 기반 비동기 대기열(Waiting Queue) 구현 및 검증
>   - [x] Step 5: Redis Sorted Set 기반 실시간 대기 순번 시스템 구현
>   - [x] Step 6: 대기열 진입 제한(Throttling) 및 유입량 제어 전략 구현
>   - [x] Step 7: SSE 기반 실시간 순번 자동 푸시 시스템 구현 및 회귀 검증
>   - [x] Step 8: k6 성능 기준선 확정 및 병목 제거
>   - [x] Step 9: 결제/좌석 점유 상태머신(홀드/확정/만료) 구현
>   - [x] Step 10: 취소/환불/재판매 대기열 연계 구현

---

## 운영 안정화 체크리스트 (Merged TODO)
---
> [!NOTE]
> 이 섹션은 기존 `prj-docs/TODO.md` 내용을 `task.md`로 통합한 항목입니다.
>
> - [x] 대기열 SSE 구독 엔드포인트 추가 (`/api/v1/waiting-queue/subscribe`)
> - [x] 대기 순번 변경 이벤트(`RANK_UPDATE`) 자동 푸시 흐름 완성
> - [x] 활성 전환 시점(`ACTIVE`) 알림 페이로드 표준화
> - [x] 타임아웃/재연결 포함 SSE 연결 수명주기 안정화
> - [x] API 변경 시 문서/HTTP/API스크립트 자동 체인 검증(pre-commit quick/strict) 구성
> - [x] API 스크립트 실행 결과 리포트 자동 생성 (`prj-docs/api-test/latest.md`)
> - [x] Step 7 API 명세/HTTP 파일/스크립트 최종 동기화 및 회귀 검증
> - [x] Step 7 운영 회귀 테스트 실행 스크립트 정비 (`scripts/api/run-step7-regression.sh`)
> - [x] Step 7 회귀 스크립트 빌드/재생성 안정화 (`STEP7_COMPOSE_BUILD=true`, `STEP7_FORCE_RECREATE=true`)

---

## 후속 백로그 (Merged TODO)
---
> [!WARNING]
> Step 7 이후 잔여 작업과 차기 단계 선행 준비 항목입니다.
>
> - [x] develop -> main 릴리즈 PR 및 Pages 최종 검증 수행 (Issue: `#28`, PR: `#46`)
> - [x] 부하 테스트(k6)를 통한 임계치 측정 및 보고서 작성 (`make test-k6`, 리포트: `prj-docs/api-test/k6-latest.md`)
>   - 완료 메모(2026-02-11): `K6_VUS=20`, `K6_DURATION=300s` 동일 조건 before/after 측정 완료.
>   - 개선 결과: `http_req_duration.p95 3.848ms -> 3.552ms(-7.68%)`, `p99 5.405ms -> 4.810ms(-11.01%)`, `http_reqs 58067 -> 58360(+0.50%)`.
> - [ ] 프론트엔드 연동 및 통합 시나리오 검증
> - [ ] 공연 조회 캐싱 전략 도입
> - [ ] 아티스트/기획사 엔티티 확장
> - [x] Auth Track A1: 소셜 로그인 OAuth2 Code 교환 백엔드(카카오/네이버) 선반영
> - [ ] 인증/인가 기반 구축 (`JWT Access/Refresh`, Role 기반 인가, 세션/토큰 정책)
> - [ ] 소셜 로그인 연동 (`Kakao OAuth2`)
> - [ ] 검색 기능 고도화 (공연 검색/필터/정렬, 페이징)
> - [ ] 결제 샌드박스 구축 (실제 과금 없이 `PENDING -> AUTHORIZED -> CAPTURED|CANCELLED|REFUNDED` 라이프사이클 검증)
> - [ ] 결제 웹훅 시뮬레이터 구축 (성공/실패/지연/중복 재전송 시나리오)

---

## 다음 단계 로드맵 (Step 8+)
---
> [!TIP]
> 기능 고도화는 스텝 단위로 진행하며, 각 스텝은 `목표 / 완료 기준 / 다음 액션`을 명시합니다.
>
> - [x] **Step 8: k6 성능 기준선 확정 및 병목 제거**
>   - 목표: `join/status/subscribe` 기준 처리량, p95, 에러율 기준선을 확정한다.
>   - 완료 기준: `prj-docs/api-test/k6-latest.md`에 before/after와 병목 원인/개선 근거를 기록한다.
>   - 완료 근거: `prj-docs/api-test/k6-before-step8.md` + `prj-docs/api-test/k6-latest.md` + `prj-docs/api-test/k6-summary-before-step8.json` + `prj-docs/api-test/k6-summary.json`.
>   - 구현 반영: `join` Redis Lua 원자 처리 + 핫패스 로그 레벨 조정(`info -> debug`).
>
> - [x] **Step 9: 결제/좌석 점유 상태머신(홀드/확정/만료) 구현**
>   - 목표: 예약 이후 결제 단계까지 상태 전이를 일관된 도메인 규칙으로 통합한다.
>   - 완료 기준: 상태 전이 다이어그램/API 명세/회귀 스크립트가 함께 갱신된다.
>   - 완료 근거:
>     - 상태머신/만료 처리: `src/main/java/com/ticketrush/domain/reservation/service/ReservationLifecycleService.java`
>     - 예약 API(v6): `src/main/java/com/ticketrush/api/controller/ReservationController.java`
>     - 예약 만료 스케줄러: `src/main/java/com/ticketrush/global/scheduler/ReservationLifecycleScheduler.java`
>     - API 명세/HTTP/스크립트: `prj-docs/api-specs/reservation-api.md`, `scripts/http/reservation.http`, `scripts/api/v8-reservation-lifecycle.sh`
>     - 실행 리포트: `prj-docs/api-test/step9-lifecycle-latest.md`
>     - 테스트: `ReservationStateMachineTest`, `ReservationLifecycleServiceIntegrationTest`, `ReservationLifecycleSchedulerTest`
>   - 검증 메모(2026-02-11): `./gradlew test --tests '*ReservationStateMachineTest' --tests '*ReservationLifecycleServiceIntegrationTest' --tests '*ReservationLifecycleSchedulerTest'` PASS.
>   - API E2E 메모(2026-02-11): `bash scripts/api/v8-reservation-lifecycle.sh` PASS (`HOLD -> PAYING -> CONFIRMED`), 로그: `.codex/tmp/ticket-core-service/step9/20260211-213008-e2e/v8-step9-e2e.log`.
>
> - [x] **Step 10: 취소/환불/재판매 대기열 연계 구현**
>   - 목표: 취소 좌석을 대기열과 안전하게 재연결하는 재판매 플로우를 완성한다.
>   - 완료 기준: 취소/환불 API + 재판매 이벤트 처리 + 데이터 정합성 테스트를 통과한다.
>   - 완료 근거:
>     - 상태 전이 확장: `src/main/java/com/ticketrush/domain/reservation/entity/Reservation.java` (`CANCELLED`, `REFUNDED`)
>     - 라이프사이클 서비스: `src/main/java/com/ticketrush/domain/reservation/service/ReservationLifecycleService.java`
>     - 예약 API(v6): `src/main/java/com/ticketrush/api/controller/ReservationController.java` (`/cancel`, `/refund`)
>     - API 명세/HTTP/스크립트: `prj-docs/api-specs/reservation-api.md`, `scripts/http/reservation.http`, `scripts/api/v9-cancel-refund-resale.sh`
>     - 실행 리포트: `prj-docs/api-test/step10-cancel-refund-latest.md`
>   - 검증 메모(2026-02-11): `./gradlew test --tests '*ReservationStateMachineTest' --tests '*ReservationLifecycleServiceIntegrationTest' --tests '*ReservationLifecycleSchedulerTest' --tests '*WaitingQueueSchedulerTest'` PASS.
>   - API E2E 메모(2026-02-11): `bash scripts/api/v9-cancel-refund-resale.sh` PASS, 로그: `.codex/tmp/ticket-core-service/step10/20260211-220150-e2e/v9-step10-e2e.log`.
>   - 다음 액션: Step 11 정책 엔진(선예매/등급/1인 제한) 모델 정의.
>
> - [x] **Step 11: 판매 정책 엔진(선예매/등급/1인 제한) 구현**
>   - 목표: 고정 로직이 아닌 정책 기반으로 판매 조건을 제어한다.
>   - 완료 기준: 정책 변경이 코드 수정 없이 설정/테이블 중심으로 반영된다.
>   - 완료 근거:
>     - 정책 테이블/검증 도메인: `src/main/java/com/ticketrush/domain/reservation/entity/SalesPolicy.java`
>     - 정책 서비스: `src/main/java/com/ticketrush/domain/reservation/service/SalesPolicyService.java`
>     - 예약 연동(hold 선검증): `src/main/java/com/ticketrush/domain/reservation/service/ReservationLifecycleService.java`
>     - 정책 API: `src/main/java/com/ticketrush/api/controller/ConcertController.java` (`PUT/GET /api/concerts/{concertId}/sales-policy`)
>     - 유저 등급 확장: `src/main/java/com/ticketrush/domain/user/User.java`, `src/main/java/com/ticketrush/domain/user/UserTier.java`
>     - 운영 스크립트/문서: `scripts/api/v10-sales-policy-engine.sh`, `scripts/http/reservation.http`, `prj-docs/api-specs/*.md`, `prj-docs/knowledge/동시성-제어-전략.md`
>     - 실행 리포트: `prj-docs/api-test/step11-sales-policy-latest.md`
>   - 검증 메모(2026-02-12): `./gradlew test --rerun-tasks --tests '*ReservationStateMachineTest' --tests '*ReservationLifecycleServiceIntegrationTest' --tests '*ReservationLifecycleSchedulerTest'` PASS (`13 tests completed, 0 failed`).
>   - API E2E 메모(2026-02-12): `bash scripts/api/v10-sales-policy-engine.sh` PASS, 로그: `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/v10-step11-e2e.log`.
>   - 다음 액션: Step 12 부정사용 방지 룰(요청 빈도/디바이스 지문/감사 로그) 초안 구현.
>
> - [x] **Step 12: 부정사용 방지/감사 추적 기능 구현**
>   - 목표: 우회/봇/중복 시도를 탐지하고 추적 가능한 감사 이력을 남긴다.
>   - 완료 기준: 차단 규칙, 감사 로그, 운영 조회 API까지 연결된다.
>   - 완료 근거:
>     - 감사 로그 엔티티/리포지토리: `src/main/java/com/ticketrush/domain/reservation/entity/AbuseAuditLog.java`, `src/main/java/com/ticketrush/domain/reservation/repository/AbuseAuditLogRepository.java`
>     - 차단/조회 서비스: `src/main/java/com/ticketrush/domain/reservation/service/AbuseAuditService.java`
>     - 차단 로그 별도 트랜잭션 보존: `src/main/java/com/ticketrush/domain/reservation/service/AbuseAuditWriter.java`
>     - 예약 연동(hold 선검증): `src/main/java/com/ticketrush/domain/reservation/service/ReservationLifecycleService.java`
>     - 운영 조회 API: `src/main/java/com/ticketrush/api/controller/ReservationController.java` (`GET /api/reservations/v6/audit/abuse`)
>     - 운영 스크립트/문서: `scripts/api/v11-abuse-audit.sh`, `scripts/http/reservation.http`, `prj-docs/api-specs/reservation-api.md`, `prj-docs/knowledge/동시성-제어-전략.md`
>     - 실행 리포트: `prj-docs/api-test/step12-abuse-audit-latest.md`
>   - 검증 메모(2026-02-12): `./gradlew test --rerun-tasks --tests '*ReservationStateMachineTest' --tests '*ReservationLifecycleServiceIntegrationTest' --tests '*ReservationLifecycleSchedulerTest'` PASS (`17 tests completed, 0 failed`).
>   - API E2E 메모(2026-02-12): `bash scripts/api/v11-abuse-audit.sh` PASS, 로그: `.codex/tmp/ticket-core-service/step12/20260212-010759-e2e/v11-step12-e2e.log`.
>   - 다음 액션: Step 13 인증/인가 + 카카오 로그인 도메인 모델(`users/auth_identities/refresh_tokens`) 설계.
>
> - [x] **Auth Track A1: 소셜 로그인 OAuth2 Code 교환 백엔드(카카오/네이버) 선반영**
>   - 목표: 프론트 OAuth 콜백 이전에 백엔드 code 교환/사용자 매핑 계약을 먼저 고정한다.
>   - 완료 기준: `authorize-url`/`exchange` API, provider client, 사용자 social 식별자, 테스트/문서/스크립트가 함께 반영된다.
>   - 완료 근거:
>     - 컨트롤러/서비스: `src/main/java/com/ticketrush/api/controller/SocialAuthController.java`, `src/main/java/com/ticketrush/domain/auth/service/SocialAuthService.java`
>     - provider client: `src/main/java/com/ticketrush/domain/auth/oauth/KakaoOAuthClient.java`, `src/main/java/com/ticketrush/domain/auth/oauth/NaverOAuthClient.java`
>     - 사용자 식별 확장: `src/main/java/com/ticketrush/domain/user/User.java`, `src/main/java/com/ticketrush/domain/user/UserRepository.java`
>     - 문서/스크립트: `prj-docs/api-specs/social-auth-api.md`, `prj-docs/knowledge/social-login-oauth-연동-전략.md`, `scripts/http/auth-social.http`, `scripts/api/v12-social-auth-contract.sh`
>     - 실행 리포트: `prj-docs/api-test/step13-social-auth-latest.md`
>   - 검증 메모(2026-02-12): `./gradlew test --tests '*SocialAuthServiceTest'` PASS.
>
> - [ ] **Step 13: 인증/인가 + 소셜 로그인(카카오) 통합**
>   - 목표: 예약/결제 API를 인증된 사용자 컨텍스트로 보호하고 소셜 로그인으로 온보딩을 단순화한다.
>   - 완료 기준: `JWT Access/Refresh`, Role 인가, `Kakao OAuth2` 로그인/회원 연동, 만료/재발급 흐름 테스트를 통과한다.
>   - 다음 액션: auth 도메인 설계(`users`, `auth_identities`, `refresh_tokens`) + 카카오 콜백 처리 API 초안 작성.
>
> - [ ] **Step 14: 프론트엔드 연동 + 검색/탐색 UX 구현**
>   - 목표: 사용자 기준의 실사용 흐름(로그인 -> 대기열 -> 예약 -> 결제/취소/환불)을 화면에서 완결한다.
>   - 완료 기준: 핵심 화면/상태 전이 UI, 공연 검색/필터/정렬, 오류/재시도 UX까지 동작한다.
>   - 다음 액션: 페이지 라우트와 API contract 매핑표 작성 후 MVP 화면 우선 구현.
>
> - [ ] **Step 15: 결제 샌드박스(무과금) + 웹훅 시뮬레이션 검증**
>   - 목표: 실제 과금 없이도 운영 결제와 유사한 상태/실패 시나리오를 재현 가능한 테스트 환경을 만든다.
>   - 완료 기준: `PaymentIntent` 상태머신, idempotency key, 서명 검증 가능한 가짜 웹훅, 중복/지연/순서역전 테스트를 통과한다.
>   - 다음 액션: `PaymentGateway` 인터페이스 + `SandboxGateway` 구현, 시나리오 스크립트(`approve/deny/timeout/retry`) 작성.

---

## 진행된 세부 작업 (Completed Details)
---
> ### 1. 동시성 제어 실험 (Concurrency Challenge)
> ---
>   - **Step 0 (락 없음)**: Race Condition 발생 확인 (30명 중 10명 중복 예약).
>   - **Step 1 (낙관적 락)**: JPA @Version을 통한 충돌 감지 및 정합성 보장 확인.
>   - **Step 2 (비관적 락)**: SELECT ... FOR UPDATE를 통한 순차 처리 및 정합성 보장 확인.
>
> ### 2. API Layer Implementation
> ---
>   - **Concert API**: 목록 조회, 옵션 조회, 예약 가능 좌석 조회 엔드포인트 구현.
>   - **Reservation API**: 예약 요청 처리, 비관적 락 로직 및 일반 예약 구현.
>   - **DTOs**: ConcertResponse, SeatResponse, ReservationRequest 등 규격화.
>
> ### 3. Core Domain & DB Layer
> ---
>   - **Domain**: Concert, Seat, Reservation, User 엔티티 및 Repository 구현.
>   - **Infrastructure**: JPA 설정 및 H2/PostgreSQL 연동 확인.
>   - **Data Init**: K-POP 콘서트 더미 데이터 초기화 로직 구현.
>
> ### 4. 프로젝트 아키텍처 정교화 및 인프라 안정화
> ---
>   - **레이어 분리**: api, domain, global 계층 구조 확립.
>   - **DTO 정규화**: 모든 Java record를 class + Lombok 스타일로 전환하여 유연성 확보.
>   - **인프라 안정화**: docker-compose.yml 헬스체크 및 의존성 최적화.
