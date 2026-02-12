# Auth Session API Specification (Auth Track A2)

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-12 08:25:00`
> - **Updated At**: `2026-02-12 08:25:00`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 0. 개요
> - 1. 세션 토큰 API
> - 2. 인증 사용자 예약 API(v7)
> - 3. 권한 정책
> - 4. 검증 스크립트
<!-- DOC_TOC_END -->

---

## 0. 개요

- 목적: 소셜 로그인 교환 결과를 실제 서비스 로그인 세션(JWT Access/Refresh)으로 연결한다.
- 기본 경로:
  - 인증: `/api/auth`
  - 예약(v7): `/api/reservations/v7`
- 토큰 형식: `Authorization: Bearer {accessToken}`

---

## 1. 세션 토큰 API

### 1.1 소셜 교환 + 세션 발급
- **Endpoint**: `POST /api/auth/social/{provider}/exchange`
- **Description**: 기존 A1의 사용자 매핑 결과에 Access/Refresh 토큰을 함께 반환한다.

**Response Example (200)**

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

### 1.2 Access Token 재발급
- **Endpoint**: `POST /api/auth/token/refresh`
- **Description**: 유효한 Refresh Token으로 Access/Refresh를 회전 발급한다.

**Request Example**

```json
{
  "refreshToken": "eyJ..."
}
```

### 1.3 로그아웃
- **Endpoint**: `POST /api/auth/logout`
- **Description**: 전달된 Refresh Token을 폐기(revoke)한다.

### 1.4 내 정보 조회
- **Endpoint**: `GET /api/auth/me`
- **Description**: Access Token으로 현재 인증 사용자의 프로필을 조회한다.

---

## 2. 인증 사용자 예약 API(v7)

### 2.1 HOLD 생성
- **Endpoint**: `POST /api/reservations/v7/holds`
- **Description**: 요청 본문의 `seatId`를 현재 인증 사용자 컨텍스트로 HOLD 처리한다.

**Request Example**

```json
{
  "seatId": 1,
  "requestFingerprint": "fp-a2-001",
  "deviceFingerprint": "device-a2-001"
}
```

### 2.2 상태 전이/조회
- `POST /api/reservations/v7/{reservationId}/paying`
- `POST /api/reservations/v7/{reservationId}/confirm`
- `POST /api/reservations/v7/{reservationId}/cancel`
- `POST /api/reservations/v7/{reservationId}/refund`
- `GET /api/reservations/v7/{reservationId}`
- `GET /api/reservations/v7/me`

모든 전이는 인증 사용자 ID를 서버 컨텍스트에서만 사용한다.

---

## 3. 권한 정책

- `USER`:
  - `/api/auth/me`
  - `/api/reservations/v7/**`
- `ADMIN`:
  - `GET /api/reservations/v7/audit/abuse`

인증 실패 시 `401`, 권한 부족 시 `403`을 반환한다.

---

## 4. 검증 스크립트

- API 체인 규칙은 Step(`v*.sh`) + Track(`a*.sh`)를 함께 사용한다.
  - A2 검증 스크립트: `scripts/api/a2-auth-track-session-guard.sh`
