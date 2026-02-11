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

### 테스트 검증 설명

이 보고서의 검증은 `prj-docs/api-test/k6-before-step8.md`(Before)와 본 문서의 `Metrics Diff`(After)를 동일 조건(`K6_VUS=20`, `K6_DURATION=300s`)으로 비교하는 방식이다. 먼저 안정성은 `http_req_failed.rate`가 `0 -> 0`으로 유지되어 성능 개선 과정에서 오류가 늘지 않았음을 확인한다. 다음으로 체감 성능은 `http_req_duration.p(95) 3.848ms -> 3.552ms`, `http_req_duration.p(99) 5.405ms -> 4.810ms`를 확인해 느린 구간 지연이 줄었는지 판단한다. 처리량은 `http_reqs.rate 193.464 -> 194.410 req/s`로 유지/상승 여부를 본다. 마지막으로 정책 일관성은 `join_accepted.count`가 `400 -> 400`으로 유지되는지 확인해 Step 6 throttling 규칙이 깨지지 않았는지 검증한다. 이 네 항목을 함께 보면, 이번 변경은 "오류 증가 없이 tail latency를 줄이고 처리량을 소폭 높인 개선"으로 판정된다.

### Verdict

- Step 8의 "코드 병목 제거 + 동일 조건 재측정 + before/after 증빙" 조건 충족.
