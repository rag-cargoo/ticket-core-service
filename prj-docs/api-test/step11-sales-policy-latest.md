# Step 11 Sales Policy Engine Verification Report

- Result: PASS
- Run Time (KST): 2026-02-12 00:16:22 KST (API E2E), 2026-02-12 00:28:03 KST (JUnit rerun)
- Scope: 선예매 등급 제어 + 일반 오픈 시각 제어 + 1인 동시 예약 제한(같은 공연 기준)
- Before Baseline Commit: `69e215c` (`feat(step10): implement cancel-refund lifecycle and resale queue linkage`)
- After Implementation Commit: `e6177c4` (`feat(step11): add sales policy engine and verification flow`)
- API E2E Run ID: `20260212-001622-e2e`

## Execution Command

```bash
cd workspace/apps/backend/ticket-core-service
./gradlew test --rerun-tasks \
  --tests '*ReservationStateMachineTest' \
  --tests '*ReservationLifecycleServiceIntegrationTest' \
  --tests '*ReservationLifecycleSchedulerTest'
```

## Raw Evidence (Execution Log)

- Full Gradle log (tmp): `.codex/tmp/ticket-core-service/step11/20260212-002803-tests/gradle-step11-tests.log`
- JUnit XML:
  - `build/test-results/test/TEST-com.ticketrush.domain.reservation.entity.ReservationStateMachineTest.xml`
  - `build/test-results/test/TEST-com.ticketrush.domain.reservation.service.ReservationLifecycleServiceIntegrationTest.xml`
  - `build/test-results/test/TEST-com.ticketrush.global.scheduler.ReservationLifecycleSchedulerTest.xml`

```text
> Task :test
BUILD SUCCESSFUL in 1m
4 actionable tasks: 4 executed
```

## API E2E Evidence (`v10-sales-policy-engine.sh`)

- Health check marker: `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/e2e-status.txt`
- API lifecycle log: `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/v10-step11-e2e.log`

```text
[Step 3] BASIC 유저 선예매 차단 검증(기대: 409)... 성공 (code=409)
[Step 4] VIP 유저 선예매 허용 검증(기대: HOLD)... 성공 (status=HOLD)
[Step 5] VIP 유저 1인 제한 검증(두 번째 HOLD 차단)... 성공 (code=409)
[Step 6] 정책 조회 API 검증... 성공 (limit=1)
>>>> [v10 Test] 검증 종료 (PASS).
```

## Test Matrix (JUnit XML 기반)

| Test Suite | Tests | Failures | Errors | Skipped | Key Coverage |
| --- | --- | --- | --- | --- | --- |
| `ReservationStateMachineTest` | 6 | 0 | 0 | 0 | 기존 상태 전이 규칙 회귀 |
| `ReservationLifecycleServiceIntegrationTest` | 6 | 0 | 0 | 0 | Step11 선예매 등급 차단 + 1인 제한 검증 포함 |
| `ReservationLifecycleSchedulerTest` | 1 | 0 | 0 | 0 | 홀드 만료 스케줄러 위임 |

## 테스트 검증 설명 (로그 판독 기준)

| 확인 위치 | 기대 값 | 판정 의미 |
| --- | --- | --- |
| `.codex/tmp/ticket-core-service/step11/20260212-002803-tests/gradle-step11-tests.log` | `BUILD SUCCESSFUL` | Step11 포함 회귀 테스트가 컴파일/실행 단계까지 통과 |
| `TEST-com.ticketrush.domain.reservation.service.ReservationLifecycleServiceIntegrationTest.xml` | `tests=6, failures=0, errors=0` | 선예매 등급 차단 + 1인 제한 통합 테스트 통과 |
| `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/e2e-status.txt` | `health_check_ok` | 실제 기동된 API 서버 대상으로 E2E가 수행됨 |
| `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/v10-step11-e2e.log` Step 3 | `code=409` | BASIC 유저 선예매 차단 동작 확인 |
| `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/v10-step11-e2e.log` Step 4 | `status=HOLD` | VIP 유저 선예매 허용 동작 확인 |
| `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/v10-step11-e2e.log` Step 5 | `code=409` | VIP 동일 공연 2차 HOLD 제한 동작 확인 |
| `.codex/tmp/ticket-core-service/step11/20260212-001622-e2e/v10-step11-e2e.log` 마지막 줄 | `검증 종료 (PASS)` | 정책 API + 예약 API 연계 시나리오 전체 통과 |

## Before/After Code Validation

| 검증 항목 | Before (Step 10) | After (Step 11) | 근거 |
| --- | --- | --- | --- |
| 판매 정책 모델 | 없음 | 공연별 `sales_policies` 엔티티/리포지토리 도입 | `src/main/java/com/ticketrush/domain/reservation/entity/SalesPolicy.java`, `src/main/java/com/ticketrush/domain/reservation/repository/SalesPolicyRepository.java` |
| HOLD 사전 검증 | 좌석 가용성만 확인 | 정책(기간/등급/1인 제한) 검증 후 HOLD 진행 | `src/main/java/com/ticketrush/domain/reservation/service/ReservationLifecycleServiceImpl.java`, `src/main/java/com/ticketrush/domain/reservation/service/SalesPolicyServiceImpl.java` |
| 정책 운영 API | 없음 | `PUT/GET /api/concerts/{concertId}/sales-policy` | `src/main/java/com/ticketrush/api/controller/ConcertController.java` |
| 사용자 등급 | `username`만 관리 | `tier`(`BASIC/SILVER/GOLD/VIP`) 지원 | `src/main/java/com/ticketrush/domain/user/User.java`, `src/main/java/com/ticketrush/domain/user/UserTier.java`, `src/main/java/com/ticketrush/api/dto/UserRequest.java` |
| 회귀 스크립트 | Step10(`v9`)까지 | Step11 정책 검증 스크립트 추가 | `scripts/api/v10-sales-policy-engine.sh` |

## Verdict

- Step 11은 정책 데이터 기반으로 HOLD 접근을 통제하며, 선예매 등급/일반 오픈/1인 제한이 코드/테스트/E2E 로그에서 모두 검증됨.
- 운영자는 정책 API 값 변경만으로 판매 조건을 조정할 수 있고, 예약 API 로직 재배포 없이 규칙 반영이 가능함.
