# Social Auth API Specification

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-12 02:15:34`
> - **Updated At**: `2026-02-12 02:15:34`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 0. 개요 (Overview)
> - 1. API 상세 명세 (Endpoint Details)
> - 2. Provider별 유의사항 (Provider Notes)
> - 3. 공통 에러 응답 (Common Error)
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
- **Description**: `authorization code`를 access token으로 교환하고, 소셜 계정 기준으로 내부 사용자를 생성/조회합니다.

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

**Response Example (200 OK)**

```json
{
  "userId": 12,
  "username": "kakao_123456789",
  "provider": "kakao",
  "socialId": "123456789",
  "email": "user@example.com",
  "displayName": "테스터",
  "newUser": true
}
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
- authorize endpoint: `https://nid.naver.com/oauth2.0/authorize`
- token endpoint: `https://nid.naver.com/oauth2.0/token`
- profile endpoint: `https://openapi.naver.com/v1/nid/me`
- `state`는 위변조 방지를 위해 필수입니다.

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
