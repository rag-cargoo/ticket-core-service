# k6 Waiting Queue Load Test Report

- Result: PASS
- Run Started (UTC): 2026-02-11 10:31:54 UTC
- API Host: `http://127.0.0.1:8080`
- Concert ID: 1
- VUs: 20
- Duration: 300s
- k6 Script: `scripts/perf/k6-waiting-queue-join.js`
- Execution Mode: `docker`
- Web Dashboard Enabled: `false`
- Web Dashboard URL: `http://127.0.0.1:5665`

## Summary Metrics

| Metric | Value |
| --- | --- |
| http_reqs.count | 58360 |
| http_req_failed.rate | 0 |
| http_req_duration.p(95) ms | 3.552138449999998 |
| http_req_duration.p(99) ms | 4.809830419999997 |
| checks.rate | 1 |
| join_accepted.count | 400 |
| join_rejected.count | 57960 |
| join_unknown.count | N/A |
| join_http_error.count | N/A |
| join_valid_status_rate.rate | 1 |

## Artifacts

- k6 raw log: `prj-docs/api-test/k6-latest.log`
- k6 summary json: `prj-docs/api-test/k6-summary.json`
- k6 web dashboard html: `prj-docs/api-test/k6-web-dashboard.html`

## Step 8 Before/After (2026-02-11)

- Baseline report: `prj-docs/api-test/k6-before-step8.md`
- Baseline summary: `prj-docs/api-test/k6-summary-before-step8.json`
- After summary: `prj-docs/api-test/k6-summary.json`

### Change Set

- `src/main/java/com/ticketrush/domain/waitingqueue/service/WaitingQueueServiceImpl.java`
  - `join` 경로를 Redis Lua 스크립트로 원자 처리하여 `active 확인 + 기존 rank 확인 + throttling + add + rank`를 단일 round trip으로 통합.
  - 재요청 사용자의 기존 rank를 그대로 반환해 불필요한 재정렬/재조회 경로를 제거.
- `src/main/java/com/ticketrush/api/waitingqueue/WaitingQueueController.java`
  - `join/status/subscribe` 요청 로그를 `info -> debug`로 조정해 핫패스 로그 I/O 비용을 완화.

### Metrics Diff (`K6_VUS=20`, `K6_DURATION=300s`)

| Metric | Before | After | Delta |
| --- | --- | --- | --- |
| http_reqs.count | 58067 | 58360 | +293 (+0.50%) |
| http_reqs.rate | 193.464 req/s | 194.410 req/s | +0.945 req/s (+0.49%) |
| http_req_duration.avg | 2.593 ms | 2.121 ms | -0.472 ms (-18.22%) |
| http_req_duration.p(95) | 3.848 ms | 3.552 ms | -0.295 ms (-7.68%) |
| http_req_duration.p(99) | 5.405 ms | 4.810 ms | -0.595 ms (-11.01%) |
| http_req_failed.rate | 0 | 0 | 변화 없음 |

### Beginner Guide: 어떤 값을 보면 되는가

- `http_reqs.rate/count`: 같은 조건에서 값이 늘면 처리량이 좋아진 것이다.
- `http_req_duration.p(95)`: 전체 요청 중 느린 5% 구간의 응답시간이다. 낮을수록 좋다.
- `http_req_duration.p(99)`: 최악에 가까운 1% 구간의 응답시간이다. 병목 체감과 가장 직접적으로 연결된다.
- `http_req_failed.rate`: 실패율이다. `0`이 유지되어야 안정성 저하 없이 개선된 것으로 본다.
- `join_accepted.count`/`join_rejected.count`: Step 6 throttling 정책 영향값이다.
  - `accepted=400`은 정책이 정상 동작한다는 뜻이다.
  - `rejected` 증가는 과부하 방지 정책이 작동한 결과이며, 성능 퇴화 신호가 아니다.

### 해석: 이번 Step 8에서 실제로 개선된 점

- 처리량: `193.464 -> 194.410 req/s` (`+0.49%`)로 소폭 상승했다.
- Tail latency:
  - `p95 3.848ms -> 3.552ms` (`-7.68%`)
  - `p99 5.405ms -> 4.810ms` (`-11.01%`)
- 안정성: `http_req_failed.rate = 0`이 before/after 모두 유지됐다.
- 정책 일관성: `join_accepted.count = 400`이 유지되어 throttling 동작이 깨지지 않았다.

### 판정 기준과 결과

| 판정 항목 | 기준 | 결과 |
| --- | --- | --- |
| 안정성 | `http_req_failed.rate == 0` 유지 | PASS (`0 -> 0`) |
| 응답속도 | `p95/p99`가 baseline 대비 악화되지 않을 것 | PASS (둘 다 개선) |
| 처리량 | `http_reqs.rate`가 baseline 대비 유지/상승 | PASS (`+0.49%`) |
| 정책 보존 | `join_accepted.count`가 정책값(400) 유지 | PASS (`400 -> 400`) |

### Verdict

- Step 8의 "코드 병목 제거 + 동일 조건 재측정 + before/after 증빙" 조건 충족.
- 초보자 관점 결론: 같은 안정성(실패율 0)을 유지한 채, 느린 구간(p95/p99) 응답을 줄였기 때문에 "실제 체감 성능이 좋아진 개선"으로 본다.
