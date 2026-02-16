# UX Track U1 Frontend MVP Playbook

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-12 18:30:52`
> - **Updated At**: `2026-02-17 02:32:01`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1. Goal and Scope
> - 2. Failure-First
> - 3. Route and API Mapping
> - 4. State Model
> - 5. Before and After
> - 6. Execution Log
<!-- DOC_TOC_END -->

---

## 1. Goal and Scope

- Goal: 로그인 이후 실제 사용자가 따라갈 수 있는 최소 UI 흐름을 고정한다.
- Scope: `src/main/resources/static/ux/u1/*` 기반 정적 페이지 MVP.
- In Scope:
  - OAuth 시작/콜백 코드 교환/토큰 저장
  - 콘서트 검색/필터/정렬/페이징 + 옵션/좌석 탐색
  - Waiting Queue `join/status/SSE subscribe` 콘솔 액션
  - `AVAILABLE` 좌석 선택과 Reservation v7 Hold 입력 자동연동
- Out of Scope:
  - 결제 샌드박스(P1) 화면 통합
  - 결제 상태 패널(`PENDING/AUTHORIZED/CAPTURED/CANCELLED/REFUNDED`)

---

## 2. Failure-First

### Failure 1: 임시 `/tmp` 경로에 프론트 산출물 보관
- Risk: 세션 종료나 워크트리 정리 시 유실되고 재검증 자산이 사라진다.
- Fix: 영구 경로 `src/main/resources/static/ux/u1`로 고정 배치.

### Failure 2: 구경로(`/u1/*`) 장기 유지로 인한 운영 혼선
- Risk: 동일 기능 URL이 2개가 되어 문서/운영 기준이 분산되고 검증 범위가 늘어난다.
- Fix: 구경로 리다이렉트 엔트리를 제거하고 `/ux/u1/*` 단일 경로만 운영한다.

### Failure 3: 좌석 선택과 Hold 요청 입력이 분리됨
- Risk: 사용자가 seatId를 수동 입력하면서 오입력/재현 불일치가 발생한다.
- Fix: Explorer에서 `AVAILABLE` 좌석 클릭 시 `seatId` 입력값을 자동 동기화.

### Failure 4: Queue 테스트를 API 도구로만 수행
- Risk: 로그인/탐색/대기열/예약의 사용자 플로우를 한 화면에서 재현하기 어렵다.
- Fix: U1 메인 화면에 Waiting Queue 섹션을 추가하고 `userId/concertId` 자동 기본값을 연동한다.

### Failure 5: callback 성공 직후 세션 유효성 미검증
- Risk: 토큰 응답이 불완전하거나 만료/오류 상태여도 브라우저에 저장되어 이후 API가 연쇄 실패한다.
- Fix: callback에서 `state` strict 검증 + token pair 확인 + `GET /api/auth/me` 검증 성공 시에만 세션 저장.

### Failure 6: callback origin 전환(`127.0.0.1` ↔ `localhost`) 시 state 유실
- Risk: provider에서 `localhost`로 돌아왔는데 최초 시작이 `127.0.0.1`이면 localStorage가 분리되어 state mismatch/누락으로 교환이 실패한다.
- Fix: callback에서 localStorage state가 없을 때 `u1_<provider>_<ts>_<nonce>` 형식 검증을 통과하면 제한적으로 진행하고 경고 로그를 남긴다.

### Failure 7: 로그인 버튼 클릭 후 사용자 체감상 무반응
- Risk: 네트워크 지연/오류 시 진행상황이 보여지지 않아 실패 원인을 파악하기 어렵다.
- Fix: `runAction` 공통 상태 배너(`HH:MM:SS`, info/ok/error 색상)와 fetch timeout 에러 메시지를 추가한다.

---

## 3. Route and API Mapping

| Route/Page | Purpose | API Chain |
| :--- | :--- | :--- |
| `/ux/u1/index.html` | 메인 콘솔 (세션 + 탐색 + 대기열 + 예약 액션) | `GET /api/auth/me`, `POST /api/auth/token/refresh`, `POST /api/auth/logout`, `GET /api/concerts/search`, `GET /api/concerts/{id}/options`, `GET /api/concerts/options/{optionId}/seats`, `POST /api/v1/waiting-queue/join`, `GET /api/v1/waiting-queue/status`, `GET /api/v1/waiting-queue/subscribe`, `POST /api/reservations/v7/*` |
| `/ux/u1/callback.html` | OAuth code exchange 처리 | `POST /api/auth/social/{provider}/exchange` |

---

## 4. State Model

- Session State:
  - `apiBase`, `accessToken`, `refreshToken`, `currentUser`
  - 저장 위치: `localStorage`
- Explorer State:
  - `concerts`, `options`, `seats`
  - `concertSearch(page,size,totalElements,totalPages,hasNext,hasFetched)`
  - `selectedConcertId`, `selectedOptionId`, `selectedSeatId`
- Queue State:
  - `queueUserId`, `queueConcertId`
  - `status`, `rank`, `activeTtlSeconds`, `lastEvent`
  - `EventSource` 연결 상태(`OPEN/CLOSED`)
- UI Contract:
  - callback(`code/state`)는 localStorage의 `oauth_state`와 일치할 때만 exchange 진행
  - 단, localStorage state가 없으면 callback `state`의 U1 포맷(`u1_<provider>_<ts>_<nonce>`) 검증 후 제한적으로 진행
  - exchange 이후 `GET /api/auth/me` 성공 시에만 로그인 완료로 간주
  - index 초기 진입 시 access token bootstrap 검증 후, 실패하면 refresh 재시도, 최종 실패 시 세션 정리
  - 콘서트 탐색은 `/api/concerts/search` 서버 검색으로 수행하고, 검색어 입력은 250ms 디바운스를 적용
  - `Prev/Next` 페이지 버튼으로 `page`를 변경하며, 현재 페이지 정보는 `Page x/y`로 표시
  - Concert 선택 시 Option 목록 reload
  - Option 선택 시 Seat 목록 reload
  - Seat 선택 시 Reservation `seatId` auto-fill
  - Queue 입력값이 비어 있으면 로그인 사용자/선택 콘서트를 자동 사용
  - SSE 이벤트(`INIT`, `RANK_UPDATE`, `ACTIVE`, `KEEPALIVE`)를 Queue State 패널과 Console에 동시 기록
  - 액션 결과는 상단 상태 배너(`HH:MM:SS`, success/error 색상) + Console에 동시 기록
  - 토큰은 UI/Console에서 원문을 노출하지 않고 길이 요약(`stored (len=...)`)만 표시

---

## 5. Before and After

### Before (나쁜 예시)
- callback이 localStorage state 누락 시 즉시 실패하여, 동일 머신에서도 `127.0.0.1`과 `localhost` 혼용 시 로그인이 불안정했다.
- 토큰이 UI 일부/로그에 부분 문자열로 노출되어 디버깅 편의성은 있었지만 보안상 불필요한 노출 위험이 있었다.
- 사용자 관점에서는 로그인 버튼 클릭 직후 진행 상태가 명확하지 않아 "무반응"으로 인식되었다.

### After (개선 결과)
- callback state 검증은 strict 기본을 유지하되, state 유실 케이스는 U1 state 포맷 검증 기반으로 제한적 허용하여 호환성을 확보했다.
- 토큰 원문 노출을 제거하고 token state/length와 pair check(`DIFFERENT` 정상)만 표시하도록 변경했다.
- 액션 상태 배너에 타임스탬프 + 성공/실패 색상을 적용해 버튼 클릭 즉시 결과를 인지할 수 있게 했다.

---

## 6. Execution Log

```text
[check] node --check src/main/resources/static/ux/u1/app.js
[check] node --check src/main/resources/static/ux/u1/callback.js
[check] playwright: social login callback round-trip (kakao/naver)
[result] callback -> index redirect success, status badge shown with HH:MM:SS
[result] token exposure reduced: UI/console now show stored (len=...)
[result] syntax validation passed
```
