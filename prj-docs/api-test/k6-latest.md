# k6 Waiting Queue Load Test Report

- Result: PASS
- Run Started (UTC): 2026-02-11 02:20:59 UTC
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
| http_reqs.count | 56700 |
| http_req_failed.rate | 0 |
| http_req_duration.p(95) ms | 7.573771649999998 |
| http_req_duration.p(99) ms | 13.44170051000001 |
| checks.rate | 1 |
| join_accepted.count | 390 |
| join_rejected.count | 56310 |
| join_unknown.count | N/A |
| join_http_error.count | N/A |
| join_valid_status_rate.rate | 1 |

## Artifacts

- k6 raw log: `prj-docs/api-test/k6-latest.log`
- k6 summary json: `prj-docs/api-test/k6-summary.json`
- k6 web dashboard html: `prj-docs/api-test/k6-web-dashboard.html`

## Analysis Notes (2026-02-11)

- 이번 실행은 Step 8 기준선 확정용(`20 VUs`, `300s`)으로 수행했고 모든 threshold를 만족했다.
- `join_rejected` 비율이 높은 것은 대기열 정책(활성 슬롯 제한)에 따른 정상 동작이다.
- 본 리포트는 **기준선(Before)** 용도로 사용하며, 다음 코드 최적화 후 동일 조건 재측정으로 **After**를 누적한다.
