# Auth Track A2 Session Guard Validation Report

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-12 09:19:00`
> - **Updated At**: `2026-02-12 09:19:00`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1) Test Scope
> - 2) Before / After
> - 3) Commands
> - 4) Execution Evidence
> - 5) Reading Guide
> - 6) Limitations
<!-- DOC_TOC_END -->

## 1) Test Scope

- 대상: Auth Track A2 (`JWT Access/Refresh`, 인증 사용자 컨텍스트, 보호 라우팅)
- 검증 관점:
  - 서비스/보안 단위 테스트가 토큰 발급/회전/인증 프로필 흐름을 보장하는가
  - 런타임 API 스크립트가 `401/400` 가드 동작을 보장하는가

## 2) Before / After

- Before (A1 기준):
  - 소셜 `exchange`는 사용자 매핑 중심 계약 검증까지만 고정
  - 보호 API에 대한 인증 가드 검증 리포트가 분리되어 있지 않음
- After (A2 반영):
  - `exchange` 결과에 `accessToken/refreshToken` 포함
  - `/api/auth/me`, `/api/reservations/v7/**` 인증 가드 동작을 테스트 + 런타임 스크립트로 검증

## 3) Commands

```bash
cd workspace/apps/backend/ticket-core-service
./gradlew test --rerun-tasks --tests '*AuthSessionServiceTest' --tests '*AuthSecurityIntegrationTest' --tests '*SocialAuthServiceTest'
bash scripts/api/a2-auth-track-session-guard.sh
```

## 4) Execution Evidence

### 4.1 Gradle Tests

- 결과: `BUILD SUCCESSFUL`
- 테스트 클래스별 결과:
  - `AuthSessionServiceTest`: `3 tests, 0 failures`
  - `AuthSecurityIntegrationTest`: `2 tests, 0 failures`
  - `SocialAuthServiceTest`: `4 tests, 0 failures`
- 합계: `9 tests, 0 failures`

### 4.2 Runtime API Script

- 스크립트: `scripts/api/a2-auth-track-session-guard.sh`
- 결과: `PASS`
- 로그 핵심:
  - `[Step 1] /api/auth/me` 무토큰 접근 차단 `성공` (`401`)
  - `[Step 2] /api/auth/token/refresh` 입력 검증 `성공` (`400`, `refresh token is required`)
  - `[Step 3] /api/reservations/v7/holds` 무토큰 접근 차단 `성공` (`401`)

### 4.3 Raw Log Path

- 실행 디렉토리: `.codex/tmp/ticket-core-service/auth-a2/20260212-091614-contract`
- 상세 로그:
  - `.codex/tmp/ticket-core-service/auth-a2/20260212-091614-contract/a2-auth-track-session-guard.log`
  - `.codex/tmp/ticket-core-service/auth-a2/20260212-091614-contract/bootrun.log`

## 5) Reading Guide

- `401 unauthorized`:
  - 액세스 토큰 없이 보호 엔드포인트 접근이 차단됨을 의미
  - 보호 경계가 정상이라는 신호
- `400 refresh token is required`:
  - 요청 스키마/필수값 검증이 정상이라는 신호
  - 잘못된 클라이언트 호출을 조기에 차단
- 위 두 조건이 동시에 PASS여야 인증 경계와 입력 경계가 함께 유지됨

## 6) Limitations

- 본 리포트는 백엔드 보호 경계 검증 중심이다.
- 실제 브라우저 소셜 로그인(E2E: provider 로그인 화면 -> callback -> token 사용)은 UX Track U1 연동 단계에서 추가 검증한다.
