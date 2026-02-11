# k6 Waiting Queue Load Test Report

- Result: PASS
- Run Started (UTC): 2026-02-11 10:20:43 UTC
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
| http_reqs.count | 58067 |
| http_req_failed.rate | 0 |
| http_req_duration.p(95) ms | 3.847615999999998 |
| http_req_duration.p(99) ms | 5.405103639999959 |
| checks.rate | 1 |
| join_accepted.count | 400 |
| join_rejected.count | 57667 |
| join_unknown.count | N/A |
| join_http_error.count | N/A |
| join_valid_status_rate.rate | 1 |

## Artifacts

- k6 raw log: `prj-docs/api-test/k6-before-step8.log`
- k6 summary json: `prj-docs/api-test/k6-summary-before-step8.json`
- k6 web dashboard html: `prj-docs/api-test/k6-web-dashboard.html`
