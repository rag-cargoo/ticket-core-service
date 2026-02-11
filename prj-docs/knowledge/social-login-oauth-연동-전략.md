# 소셜 로그인 OAuth 연동 전략 (Step 13)

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-12 02:15:34`
> - **Updated At**: `2026-02-12 02:15:34`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1. 목표와 범위
> - 2. Failure-First (실패 사례 먼저)
> - 3. Before / After
> - 4. 설계 핵심
> - 5. Execution Log
<!-- DOC_TOC_END -->

---

## 1. 목표와 범위

- 목표: 카카오/네이버 소셜 계정으로 로그인할 수 있도록 백엔드 OAuth2 Code 교환 체인을 구현한다.
- 범위: 백엔드 API(`authorize-url`, `code exchange`)와 내부 사용자 매핑까지.
- 비범위: 프론트 UI, 자체 세션/JWT 발급, 운영 배포 파이프라인.

---

## 2. Failure-First (실패 사례 먼저)

### 실패 1: 프론트에서 Access Token까지 직접 처리
- 문제: 토큰이 브라우저/로그에 남아 유출 위험이 커진다.
- 개선: 백엔드가 code를 받아 provider와 직접 통신하도록 전환.

### 실패 2: Provider별 코드 분기만 있고 사용자 매핑 규칙이 없음
- 문제: 동일 소셜 계정이 매 로그인마다 신규 사용자로 생성될 수 있다.
- 개선: `(socialProvider, socialId)` 고유 식별 기반 upsert로 통일.

### 실패 3: 네이버 `state`를 강제하지 않음
- 문제: CSRF/요청 위변조 검증 근거가 약해진다.
- 개선: 네이버 code 교환 시 `state` 필수 검증.

---

## 3. Before / After

### Before (Bad Practice)
```java
// 로그인 화면에서 provider access token을 직접 받아 서버로 전달
POST /api/auth/login
{
  "provider": "kakao",
  "accessToken": "..."
}
```

- 토큰 수명/유출 제어가 어렵다.
- provider API 오류/재시도 정책이 프론트로 분산된다.

### After (Best Practice)
```java
// 1) authorize-url 발급
GET /api/auth/social/{provider}/authorize-url

// 2) provider가 준 code를 서버에서 교환
POST /api/auth/social/{provider}/exchange
{
  "code": "...",
  "state": "..."
}
```

- 토큰 교환 책임을 서버로 집중.
- 사용자 매핑 규칙과 예외 처리가 단일 서비스에 고정.

---

## 4. 설계 핵심

- Provider 전략 분리:
  - `SocialOAuthClient` 인터페이스
  - 구현체: `KakaoOAuthClient`, `NaverOAuthClient`
- 사용자 식별 확장:
  - `User.socialProvider`, `User.socialId`, `User.email`, `User.displayName`
  - 유니크 제약: `(social_provider, social_id)`
- 서비스 책임:
  - `SocialAuthService`
  - `authorize-url` 생성(state 자동 생성)
  - `exchange` 수행 후 신규 생성/기존 갱신

---

## 5. Execution Log

```text
[test] ./gradlew test --tests '*SocialAuthServiceTest'
[result] 4 tests completed, 0 failed
[verified]
- 첫 로그인 신규 사용자 생성
- 재로그인 시 기존 사용자 재사용 + 프로필 갱신
- authorize-url state 자동 생성
- 네이버 code 교환 시 state 필수 검증
```
