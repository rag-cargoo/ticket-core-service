# API Script Test Guide

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-12 02:15:34`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1. 실행 방법
> - 2. 산출물
> - 3. 트러블슈팅 기준
> - 4. Step 7 회귀 실행 스크립트
> - 5. k6 부하 테스트 실행
> - 6. Step 9 상태머신 검증 실행
> - 7. Step 10 취소/환불/재판매 연계 검증 실행
> - 8. Step 11 판매 정책 엔진 검증 실행
> - 9. Step 12 부정사용 방지/감사 추적 검증 실행
> - 10. Auth Track: 소셜 로그인 계약 검증 실행
> - 11. Playwright MCP로 k6 HTML 열기
<!-- DOC_TOC_END -->

`scripts/api/*.sh`와 `scripts/perf/*` 실행 검증과 결과 기록 규칙입니다.

---

## 1. 실행 방법

```bash
cd workspace/apps/backend/ticket-core-service
make test-suite
```

- 내부적으로 `scripts/api/run-api-script-tests.sh`를 호출합니다.
- 기본 실행 세트는 `v1`~`v11` 스크립트입니다.
- 기본 헬스체크 URL은 `http://127.0.0.1:8080/api/concerts` 입니다.
- 필요하면 `API_SCRIPT_HEALTH_URL` 환경변수로 변경할 수 있습니다.
- 기존 환경과의 호환을 위해 `TICKETRUSH_HEALTH_URL`도 별칭으로 지원합니다.

---

## 2. 산출물

- 최신 실행 리포트: `prj-docs/api-test/latest.md`
- 커밋 체인에서 `scripts/api/*.sh` 변경이 감지되면 위 리포트를 함께 stage해야 합니다.

---

## 3. 트러블슈팅 기준

1. 백엔드 헬스체크 실패 시 앱/인프라를 먼저 기동한다.
2. 스크립트 실패 시 `latest.md`의 실패 로그 요약을 확인한다.
3. 수정 후 테스트 재실행하여 리포트를 최신화한다.
4. 로컬 Kafka 접속 실패 시 `./gradlew bootRun --args='--spring.profiles.active=local --spring.kafka.bootstrap-servers=localhost:9092'`로 재기동한다.

---

## 4. Step 7 회귀 실행 스크립트

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/run-step7-regression.sh
```

- 실행 내용:
  - 인프라/앱 기동 후 `v7-sse-rank-push.sh` 회귀 검증
  - 결과 리포트(`prj-docs/api-test/latest.md`)와 런타임 로그(`.codex/tmp/ticket-core-service/step7/<run-id>/step7-regression.log`) 생성
- 기본 안정화 옵션:
  - `STEP7_COMPOSE_BUILD=true` (기본): app 이미지를 다시 빌드하여 로컬 코드 반영 보장
  - `STEP7_FORCE_RECREATE=true` (기본): `down -> up` 재생성으로 docker-compose 재기동 오류 회피
  - `STEP7_KEEP_ENV=true|false` (기본 false): 검증 후 compose 환경 유지 여부
  - `STEP7_LOG_FILE=/custom/path.log`: 로그 파일 경로 강제 지정
- CI(Jenkins/GitHub Actions) 도입 시에는 위 스크립트를 그대로 호출해 동일 절차를 재사용한다.

---

## 5. k6 부하 테스트 실행

```bash
cd workspace/apps/backend/ticket-core-service
make test-k6
```

- 내부적으로 `scripts/perf/run-k6-waiting-queue.sh`를 호출합니다.
- 기본 시나리오는 `scripts/perf/k6-waiting-queue-join.js`이며, `POST /api/v1/waiting-queue/join` 부하를 측정합니다.
- 기본 파라미터:
  - `K6_VUS=60`
  - `K6_DURATION=60s`
- Step 8 권장 재현(동일 조건 before/after):
  - `K6_VUS=20 K6_DURATION=300s make test-k6`
  - `cp prj-docs/api-test/k6-latest.md prj-docs/api-test/k6-before-step8.md`
  - `cp prj-docs/api-test/k6-summary.json prj-docs/api-test/k6-summary-before-step8.json`
  - 코드 변경 후 동일 명령 재실행
- 결과 산출물:
  - `prj-docs/api-test/k6-latest.md`
  - `prj-docs/api-test/k6-before-step8.md` (baseline 보관 시)
  - `.codex/tmp/ticket-core-service/k6/<run-id>/k6-latest.log`
  - `prj-docs/api-test/k6-summary.json`
  - `prj-docs/api-test/k6-summary-before-step8.json` (baseline 보관 시)
  - `.codex/tmp/ticket-core-service/k6/<run-id>/k6-web-dashboard.html` (대시보드 활성 시)
- 실행 환경에 로컬 `k6`가 없으면 Docker(`grafana/k6`) fallback으로 자동 실행합니다.
- Docker fallback 기본 네트워크는 `host`이며, 필요 시 `K6_DOCKER_NETWORK`로 변경합니다.
- 임시 산출물 기본 루트는 `.codex/tmp/ticket-core-service/k6/`이며, 필요 시 `CODEX_TMP_DIR`로 변경합니다.
- 웹 대시보드를 함께 보고 싶으면 아래처럼 실행합니다.

```bash
cd workspace/apps/backend/ticket-core-service
make test-k6-dashboard
```

- 실행 중 대시보드 URL: `http://127.0.0.1:5665`

---

## 6. Step 9 상태머신 검증 실행

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/v8-reservation-lifecycle.sh
```

- 검증 흐름:
  - `HOLD` 생성
  - `PAYING` 전이
  - `CONFIRMED` 전이
  - 최종 상태 조회
- Step 9 실행 리포트:
  - `prj-docs/api-test/step9-lifecycle-latest.md`

---

## 7. Step 10 취소/환불/재판매 연계 검증 실행

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/v9-cancel-refund-resale.sh
```

- 검증 흐름:
  - 대기 유저 큐 진입(`WAITING`)
  - `HOLD -> PAYING -> CONFIRMED` 생성
  - `CANCELLED` 전이 + 대기열 상위 유저 `ACTIVE` 승격
  - `REFUNDED` 전이 + 최종 상태 확인
- Step 10 실행 리포트:
  - `prj-docs/api-test/step10-cancel-refund-latest.md`

---

## 8. Step 11 판매 정책 엔진 검증 실행

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/v10-sales-policy-engine.sh
```

- 검증 흐름:
  - `PUT /api/concerts/{concertId}/sales-policy` 정책 설정
  - BASIC 유저 선예매 차단(`409`)
  - VIP 유저 선예매 허용(`201 HOLD`)
  - VIP 유저 두 번째 HOLD 차단(`409`, 1인 제한)
  - 정책 조회 API 응답 일치 확인
- Step 11 실행 리포트:
  - `prj-docs/api-test/step11-sales-policy-latest.md`

---

## 9. Step 12 부정사용 방지/감사 추적 검증 실행

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/v11-abuse-audit.sh
```

- 검증 흐름:
  - 유저별 요청 빈도 초과 차단(`RATE_LIMIT_EXCEEDED`)
  - 중복 `requestFingerprint` 차단(`DUPLICATE_REQUEST_FINGERPRINT`)
  - 다계정 `deviceFingerprint` 차단(`DEVICE_FINGERPRINT_MULTI_ACCOUNT`)
  - 감사 조회 API(`GET /api/reservations/v6/audit/abuse`)에서 차단 사유 조회 확인
- Step 12 실행 리포트:
  - `prj-docs/api-test/step12-abuse-audit-latest.md`

---

## 10. Auth Track: 소셜 로그인 계약 검증 실행

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/v12-social-auth-contract.sh
```

- 검증 흐름:
  - 카카오 authorize-url 조회 계약 확인
  - 네이버 authorize-url + state 생성 계약 확인
  - exchange 입력값 검증(`code`/`state`) 400 응답 계약 확인
- 실행 리포트:
  - `prj-docs/api-test/auth-track-a1-social-auth-latest.md`

---

## 11. Auth Track A2 인증 세션/가드 검증 실행

```bash
cd workspace/apps/backend/ticket-core-service
bash scripts/api/a2-auth-track-session-guard.sh
```

- 검증 흐름:
  - `/api/auth/me` 무토큰 접근 차단(`401`)
  - `POST /api/auth/token/refresh` 입력 검증(`400`, `refresh token is required`)
  - `POST /api/reservations/v7/holds` 무토큰 접근 차단(`401`)
- 실행 리포트:
  - `prj-docs/api-test/auth-track-a2-session-guard-latest.md` (생성 예정)

---

## 12. Playwright MCP로 k6 HTML 열기

`k6-web-dashboard.html`은 로컬 파일이므로 Playwright MCP에서 `file://` 직접 열기가 실패할 수 있습니다.
표준 절차는 "로컬 HTTP 서빙 + MCP `navigate`" 입니다.

1. k6 산출물 생성

```bash
cd workspace/apps/backend/ticket-core-service
make test-k6
```

2. Chrome를 CDP 포트로 실행

```bash
setsid google-chrome --remote-debugging-port=9222 --user-data-dir=/tmp/chrome-mcp-profile --no-first-run --no-default-browser-check about:blank >/tmp/playwright_chrome.log 2>&1 < /dev/null &
curl -sS http://127.0.0.1:9222/json/version
```

3. k6 산출물 디렉토리를 로컬 HTTP로 서빙

```bash
cd workspace/apps/backend/ticket-core-service
repo_root="$(git rev-parse --show-toplevel)"
latest_run_dir="$(ls -1dt "$repo_root"/.codex/tmp/ticket-core-service/k6/* 2>/dev/null | head -n 1)"
nohup python3 -m http.server 18080 --bind 127.0.0.1 --directory "$latest_run_dir" >/tmp/k6-http.log 2>&1 &
curl -sS -I http://127.0.0.1:18080/k6-web-dashboard.html | head -n 1
```

4. Playwright MCP에서 아래 URL로 이동

```text
http://127.0.0.1:18080/k6-web-dashboard.html
```

5. 작업 후 정리(선택)

```bash
pkill -f "http.server 18080"
pkill -f "google-chrome.*remote-debugging-port=9222"
```

실패 시 `skills/aki-mcp-playwright/references/troubleshooting.md`의 "`Local HTML Cannot Be Opened Directly by MCP`" 항목을 확인합니다.
