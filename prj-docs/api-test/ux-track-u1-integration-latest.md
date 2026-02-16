# UX Track U1 Integration Scenario Validation Report

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-17 03:50:05`
> - **Updated At**: `2026-02-17 03:52:10`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1) Test Scope
> - 2) Test Environment
> - 3) Commands
> - 4) Result Matrix
> - 5) Execution Evidence
> - 6) Limitations
<!-- DOC_TOC_END -->

## 1) Test Scope

- 대상: `UX Track U1`의 핵심 사용자 흐름
- 검증 범위:
  - 소셜 로그인 계약 API(v12)
  - 인증 가드(a2)
  - 콘서트 setup/search/options/seats
  - Waiting Queue join/status/subscribe(SSE)
  - Reservation v6 상태머신(HOLD -> PAYING -> CONFIRMED)
- 제외 범위:
  - Payment Track P1(보류 상태)
  - 실제 외부 OAuth 로그인 화면(E2E 브라우저 입력)

## 2) Test Environment

- Backend Run Mode: `local` (`./gradlew bootRun`)
- Infra: docker-compose로 `postgres-db`, `redis`, `zookeeper`, `kafka` 기동
- OAuth 계약 검증용 더미 환경변수:
  - `KAKAO_CLIENT_ID=dummy-kakao-client`
  - `KAKAO_CLIENT_SECRET=dummy-kakao-secret`
  - `KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao`
  - `NAVER_CLIENT_ID=dummy-naver-client`
  - `NAVER_CLIENT_SECRET=dummy-naver-secret`
  - `NAVER_REDIRECT_URI=http://localhost:8080/login/oauth2/code/naver`
  - `NAVER_SERVICE_URL=http://localhost:8080`

## 3) Commands

```bash
cd workspace/apps/backend/ticket-core-service

bash scripts/api/v12-social-auth-contract.sh
bash scripts/api/a2-auth-track-session-guard.sh
bash scripts/api/v5-waiting-queue.sh
bash scripts/api/v8-reservation-lifecycle.sh

# U1 확장 필드 setup + server search + queue sse smoke
curl -X POST http://127.0.0.1:8080/api/concerts/setup ...
curl "http://127.0.0.1:8080/api/concerts/search?...&agencyName=..."
curl "http://127.0.0.1:8080/api/v1/waiting-queue/subscribe?..."
```

## 4) Result Matrix

- `rc_v12=0`
- `rc_a2=0`
- `rc_v5=0`
- `rc_v8=0`
- `setup_code=200`
- `search_code=200`
- `options_code=200`
- `seats_code=200`
- `join_code=200`
- `status_code=200`
- `rc_sse=0`
- **overall=PASS**

## 5) Execution Evidence

### 5.1 Concert Search (server-side)

```json
{"items":[{"id":2,"title":"U1_INTEGRATION_1771267727","artistName":"U1 Artist 1771267727","artistId":2,"artistDisplayName":"U1 Display 1771267727","artistGenre":"Indie","artistDebutDate":"2020-01-01","agencyName":"U1 Agency 1771267727","agencyCountryCode":"KR","agencyHomepageUrl":"https://example.com/u1/1771267727"}],"page":0,"size":5,"totalElements":1,"totalPages":1,"hasNext":false}
```

### 5.2 Queue SSE Snippet

```text
event:INIT
data:Connected for Queue: 2

event:RANK_UPDATE
data:{"userId":9090,"concertId":2,"status":"WAITING","rank":1,"activeTtlSeconds":0,...}

event:KEEPALIVE
```

### 5.3 Raw Log Path

- `.codex/tmp/ticket-core-service/u1/20260217-035005-u1-integration/`
  - `u1-v12-social.log`
  - `u1-a2-guard.log`
  - `u1-v5-queue.log`
  - `u1-v8-lifecycle.log`
  - `u1-integration-summary.env`

## 6) Limitations

- 본 검증은 `U1 핵심 API 통합 시나리오` 기준이며, 결제(P1)는 의사결정 대기(`HOLD`)로 제외됨.
- 실제 Kakao/Naver 로그인 폼 입력을 포함한 브라우저 E2E는 운영 자격정보/리다이렉트 정책 확정 후 별도 Playwright 검증이 필요함.
