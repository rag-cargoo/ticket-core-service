# Step 9 Reservation Lifecycle Verification Report

- Result: PASS
- Run Time (UTC): 2026-02-11 12:10:34 UTC
- Scope: `HOLD -> PAYING -> CONFIRMED|EXPIRED` 상태 전이 + TTL 만료 복구 + 스케줄러 위임
- Before Baseline Commit: `dd75e50` (`docs(step8): consolidate k6 verification explanation`)
- After Implementation Commit: `7d0ff85` (`feat(step9): implement reservation lifecycle state machine`)
- API E2E Run ID: `20260211-213008-e2e`

## Execution Command

```bash
cd workspace/apps/backend/ticket-core-service
./gradlew test --rerun-tasks \
  --tests '*ReservationStateMachineTest' \
  --tests '*ReservationLifecycleServiceIntegrationTest' \
  --tests '*ReservationLifecycleSchedulerTest'
```

## Raw Evidence (Execution Log)

- Full Gradle log (tmp): `.codex/tmp/ticket-core-service/step9/20260211-210832-rerun/gradle-step9-tests.log`
- JUnit XML:
  - `build/test-results/test/TEST-com.ticketrush.domain.reservation.entity.ReservationStateMachineTest.xml`
  - `build/test-results/test/TEST-com.ticketrush.domain.reservation.service.ReservationLifecycleServiceIntegrationTest.xml`
  - `build/test-results/test/TEST-com.ticketrush.global.scheduler.ReservationLifecycleSchedulerTest.xml`

```text
> Task :test
BUILD SUCCESSFUL in 58s
4 actionable tasks: 4 executed
```

## API E2E Evidence (`v8-reservation-lifecycle.sh`)

- Health check marker: `.codex/tmp/ticket-core-service/step9/20260211-213008-e2e/e2e-status.txt`
- API lifecycle log: `.codex/tmp/ticket-core-service/step9/20260211-213008-e2e/v8-step9-e2e.log`

```text
[Step 2] HOLD 생성... 성공 (reservationId=1, status=HOLD)
[Step 3] PAYING 전이... 성공 (status=PAYING)
[Step 4] CONFIRMED 전이... 성공 (status=CONFIRMED)
[Step 5] 상태 조회... 성공 (status=CONFIRMED)
>>>> [v8 Test] 검증 종료 (PASS).
```

## 테스트 검증 설명 (판정 기준)

| 확인 위치 | 기대 값 | 해석 |
| --- | --- | --- |
| `.codex/tmp/ticket-core-service/step9/20260211-213008-e2e/e2e-status.txt` | `health_check_ok` | 백엔드가 실제로 기동된 상태에서 테스트가 수행되었음을 의미 |
| `.../v8-step9-e2e.log` Step 2 | `status=HOLD` | 초기 점유 상태 전이가 정상인지 확인 |
| `.../v8-step9-e2e.log` Step 3 | `status=PAYING` | 결제 진행 상태 전이가 정상인지 확인 |
| `.../v8-step9-e2e.log` Step 4/5 | `status=CONFIRMED` | 최종 확정 상태와 조회 결과가 일치하는지 확인 |
| `.../v8-step9-e2e.log` 마지막 줄 | `검증 종료 (PASS)` | 전체 시나리오(유저 생성 -> 좌석 조회 -> HOLD -> PAYING -> CONFIRMED)가 중간 실패 없이 완료됨 |
| `build/test-results/test/*.xml` + Gradle 요약 | `Failures=0`, `Errors=0`, `BUILD SUCCESSFUL` | 단위/통합/스케줄러 테스트가 모두 통과했음을 의미 |

## Test Matrix (JUnit XML 기반)

| Test Suite | Tests | Failures | Errors | Skipped | Key Coverage |
| --- | --- | --- | --- | --- | --- |
| `ReservationStateMachineTest` | 4 | 0 | 0 | 0 | 상태 전이 허용/금지 규칙 (`HOLD/PAYING/CONFIRMED/EXPIRED`) |
| `ReservationLifecycleServiceIntegrationTest` | 2 | 0 | 0 | 0 | 홀드 생성/확정 + TTL 만료 시 `EXPIRED` 및 좌석 `AVAILABLE` 복구 |
| `ReservationLifecycleSchedulerTest` | 1 | 0 | 0 | 0 | 만료 스캔 스케줄러가 서비스 위임 호출하는지 검증 |

## Before/After Code Validation

| 검증 항목 | Before (`dd75e50`) | After (`7d0ff85`) | 근거 |
| --- | --- | --- | --- |
| 예약 상태 enum | `PENDING, CONFIRMED, CANCELLED` | `PENDING, HOLD, PAYING, CONFIRMED, EXPIRED, CANCELLED` | `src/main/java/com/ticketrush/domain/reservation/entity/Reservation.java` |
| Step 9 API(v6) | 없음 | `POST /v6/holds`, `POST /v6/{id}/paying`, `POST /v6/{id}/confirm`, `GET /v6/{id}` | `src/main/java/com/ticketrush/api/controller/ReservationController.java` |
| TTL 만료 처리 | 없음 | `expireTimedOutHolds()` 구현 + 스케줄러 주기 실행 | `src/main/java/com/ticketrush/domain/reservation/service/ReservationLifecycleService.java`, `src/main/java/com/ticketrush/global/scheduler/ReservationLifecycleScheduler.java` |
| 운영 회귀 스크립트 | 없음 | `v8-reservation-lifecycle.sh` 추가 | `scripts/api/v8-reservation-lifecycle.sh` |
| Step 9 테스트 | 없음 | 단위/통합/스케줄러 테스트 3종 추가 | `src/test/java/com/ticketrush/domain/reservation/entity/ReservationStateMachineTest.java`, `src/test/java/com/ticketrush/domain/reservation/service/ReservationLifecycleServiceIntegrationTest.java`, `src/test/java/com/ticketrush/global/scheduler/ReservationLifecycleSchedulerTest.java` |

## Verdict

- Step 9는 코드 구현, 상태 전이 검증, 만료 복구 검증, 스케줄러 위임 검증까지 모두 PASS로 확인됨.
- 단순 "코드 추가"가 아니라 `Before(부재)` 대비 `After(구현+검증)`가 실행 로그와 테스트 결과로 증빙됨.
