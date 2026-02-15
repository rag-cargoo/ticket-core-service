# Social Auth API Specification

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-12 02:15:34`
> - **Updated At**: `2026-02-16 01:24:08`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 0. 개요 (Overview)
> - 1. API 상세 명세 (Endpoint Details)
> - 2. Provider별 유의사항 (Provider Notes)
> - 3. 공통 에러 응답 (Common Error)
> - 4. UX Track U1 클라이언트 처리 계약
<!-- DOC_TOC_END -->

이 문서는 카카오/네이버 OAuth2 Authorization Code 기반 소셜 로그인 백엔드 연동 규격을 정의합니다.

---

## 0. 개요 (Overview)

- 목적: 프론트엔드가 획득한 `authorization code`를 백엔드에서 교환하여 소셜 계정을 내부 사용자로 매핑한다.
- 대상 Provider: `kakao`, `naver`
- 기본 경로: `/api/auth/social`

---

## 1. API 상세 명세 (Endpoint Details)

### 1.1. Provider 인가 URL 조회
- **Endpoint**: `GET /api/auth/social/{provider}/authorize-url`
- **Description**: OAuth 인가 화면으로 이동할 URL을 반환합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `provider` | String | Yes | `kakao` 또는 `naver` |
| Query | `state` | String | No | 요청 상관관계 식별값. 미입력 시 서버 생성 |

**Request Example**

```bash
GET /api/auth/social/kakao/authorize-url
GET /api/auth/social/naver/authorize-url?state=my-state-001
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `provider` | String | 요청 provider (`kakao`/`naver`) |
| `state` | String | 최종 authorize 요청에 포함되는 state |
| `authorizeUrl` | String | provider 인가 화면 URL |

**Response Example (200 OK)**

```json
{
  "provider": "naver",
  "state": "my-state-001",
  "authorizeUrl": "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=...&redirect_uri=...&state=my-state-001"
}
```

---

### 1.2. Authorization Code 교환 및 사용자 매핑
- **Endpoint**: `POST /api/auth/social/{provider}/exchange`
- **Description**: `authorization code`를 access token으로 교환하고, 소셜 계정 기준으로 내부 사용자를 생성/조회한 뒤 Access/Refresh 토큰을 함께 발급합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `provider` | String | Yes | `kakao` 또는 `naver` |
| Body | `code` | String | Yes | OAuth authorization code |
| Body | `state` | String | Conditional | 네이버는 필수, 카카오는 선택 |

**Request Example**

```json
{
  "code": "authorization-code-from-provider",
  "state": "my-state-001"
}
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `userId` | Long | 내부 사용자 ID |
| `username` | String | 내부 username |
| `provider` | String | 소셜 provider |
| `socialId` | String | provider 사용자 식별자 |
| `email` | String | 이메일(없을 수 있음) |
| `displayName` | String | 표시 이름 |
| `role` | String | 권한(`USER` 등) |
| `newUser` | Boolean | 신규 생성 여부 |
| `tokenType` | String | 토큰 타입(`Bearer`) |
| `accessToken` | String | 액세스 토큰 |
| `refreshToken` | String | 리프레시 토큰 |
| `accessTokenExpiresInSeconds` | Long | 액세스 토큰 만료(초) |
| `refreshTokenExpiresInSeconds` | Long | 리프레시 토큰 만료(초) |

**Response Example (200 OK)**

```json
{
  "userId": 12,
  "username": "kakao_123456789",
  "provider": "kakao",
  "socialId": "123456789",
  "email": "user@example.com",
  "displayName": "테스터",
  "role": "USER",
  "newUser": true,
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "accessTokenExpiresInSeconds": 1800,
  "refreshTokenExpiresInSeconds": 1209600
}
```

추가 인증 세션 API(`refresh`, `logout`, `me`)는 `prj-docs/api-specs/auth-session-api.md`를 참조하세요.

---

### 1.3. Provider Callback Redirect (U1 호환)
- **Endpoint**: `GET /login/oauth2/code/{provider}`
- **Description**: provider callback을 U1 콜백 화면으로 리다이렉트하며, 원본 query(`code`, `state`)를 전달하고 `provider`가 없으면 자동 주입합니다.
  - 기본값: `/ux/u1/callback.html` (동일 도메인)
  - 설정값: `U1_CALLBACK_URL` (예: `https://ui.example.com/ux/u1/callback.html`, 분리 도메인)

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `provider` | String | Yes | `kakao` 또는 `naver` |
| Query | `code` | String | Yes | OAuth authorization code |
| Query | `state` | String | Conditional | 일반적으로 필수(특히 naver) |

**Request Example**

```bash
GET /login/oauth2/code/naver?code=abc123&state=u1_naver_1770963678337_d4xmuaov06
```

**Response Summary (302 Found)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `Location` | String | `/ux/u1/callback.html?...` 또는 `https://<frontend>/ux/u1/callback.html?...` 형태의 리다이렉트 URL |

**Response Example**

```text
302 Found
Location: /ux/u1/callback.html?code=abc123&state=u1_naver_1770963678337_d4xmuaov06&provider=naver
```

분리 도메인 예시 (`U1_CALLBACK_URL=https://ui.example.com/ux/u1/callback.html`):

```text
302 Found
Location: https://ui.example.com/ux/u1/callback.html?code=abc123&state=u1_naver_1770963678337_d4xmuaov06&provider=naver
```

---

## 2. Provider별 유의사항 (Provider Notes)

### 2.1. Kakao
- 환경 변수:
  - `KAKAO_CLIENT_ID`
  - `KAKAO_CLIENT_SECRET`
  - `KAKAO_REDIRECT_URI`
- authorize endpoint: `https://kauth.kakao.com/oauth/authorize`
- token endpoint: `https://kauth.kakao.com/oauth/token`
- profile endpoint: `https://kapi.kakao.com/v2/user/me`

### 2.2. Naver
- 환경 변수:
  - `NAVER_CLIENT_ID`
  - `NAVER_CLIENT_SECRET`
  - `NAVER_REDIRECT_URI`
  - `NAVER_SERVICE_URL`
- authorize endpoint: `https://nid.naver.com/oauth2.0/authorize`
- token endpoint: `https://nid.naver.com/oauth2.0/token`
- profile endpoint: `https://openapi.naver.com/v1/nid/me`
- `state`는 위변조 방지를 위해 필수입니다.

### 2.3. U1 Callback Redirect 설정
- 환경 변수:
  - `U1_CALLBACK_URL` (optional, default: `/ux/u1/callback.html`)
- 권장:
  - 백엔드/프론트 동일 도메인: default 유지
  - 백엔드/프론트 분리 도메인: 프론트 callback 절대 URL 지정

---

## 3. 공통 에러 응답 (Common Error)

```json
{
  "status": 400,
  "message": "authorization code is required"
}
```

대표 오류 사례:
- `authorization code is required`
- `state is required for naver token exchange`
- `Missing oauth config: KAKAO_CLIENT_ID`

---

## 4. UX Track U1 클라이언트 처리 계약

`src/main/resources/static/ux/u1/callback.js` 기준 권장 처리 순서:

1. callback query의 `code`, `state`를 읽는다.
2. localStorage의 기대값(`ticketrush_u1_oauth_state`)과 `state` 일치 여부를 우선 strict 검증한다.
3. localStorage 값이 비어 있는 경우에는 `state`가 U1 포맷(`u1_<provider>_<ts>_<nonce>`)인지 검증한 뒤 제한적으로 진행한다(예: `127.0.0.1`↔`localhost` origin 전환).
4. `POST /api/auth/social/{provider}/exchange` 성공 후 `accessToken`, `refreshToken` 존재를 확인한다.
5. 발급된 `accessToken`으로 `GET /api/auth/me`를 호출해 인증 세션 유효성을 즉시 확인한다.
6. 위 단계가 모두 성공한 경우에만 localStorage 세션(`access/refresh/authUser`)을 최종 저장하고 메인 콘솔로 이동한다.

실패 시에는 부분 저장된 토큰/사용자 스냅샷을 제거하고 오류를 callback 콘솔에 기록한다.
U1 메인 콘솔(`index.html`)의 상단 액션 상태는 `HH:MM:SS` 타임스탬프 + 성공(초록)/실패(빨강)으로 표시하며, 콘솔 로그에서는 토큰 원문 대신 길이 요약(`stored (len=...)`)만 기록한다.
