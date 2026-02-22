# Ticket Core Service README

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-09 00:33:02`
> - **Updated At**: `2026-02-22 22:23:50`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 프로젝트 개요
> - 실행 방법
> - 검증/운영 포인트
> - 대표 문서 링크
<!-- DOC_TOC_END -->

이 문서는 `workspace/apps/backend/ticket-core-service` 프로젝트의 소개, 실행 방법, 검증 진입점을 제공합니다.

---

## 프로젝트 개요

- 목적: 선착순 티켓 예약 도메인에서 동시성 제어와 대기열 처리의 정합성을 확보하는 백엔드 코어 서비스
- 상태: `진행중` (핵심 시나리오 구현 완료, 운영/성능 고도화 진행)
- 기술 스택: Java 17, Spring Boot 3.4.1, PostgreSQL, Redis, Kafka

---

## 실행 방법

### 1. 인프라 실행
```bash
docker-compose up -d --build --scale app=1
```

```bash
make compose-up APP_REPLICAS=1
```

### 1-1. 분산 스케일 실행 예시
```bash
docker-compose up -d --build --scale app=3
```

```bash
make compose-up APP_REPLICAS=3
```

### 1-2. 프론트 포함 실행 (선택)
```bash
docker-compose --profile frontend up -d --build --scale app=1
```

- 기본 외부 진입점은 LB(`nginx-lb`)의 `http://127.0.0.1:18080`입니다.
- 프론트 컨테이너(`ticket-web-client`)는 `http://127.0.0.1:5173`로 노출됩니다.

### 2. 애플리케이션 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. API 스크립트 회귀 실행
```bash
make test-suite
```

### 4. k6 부하 테스트 실행
```bash
make test-k6
```

### 4-1. 분산 k6 실행
```bash
make test-k6-distributed
```

### 5. Auth-Social CI-safe 파이프라인 테스트
```bash
make test-auth-social-pipeline
```

### 6. Auth-Social Real Provider E2E 테스트 (선택)
```bash
APP_AUTH_SOCIAL_REAL_E2E_ENABLED=true \
AUTH_REAL_E2E_PROVIDER=kakao \
AUTH_REAL_E2E_PREPARE_ONLY=true \
make test-auth-social-real-provider
```

- 준비 단계(`AUTH_REAL_E2E_PREPARE_ONLY=true`)에서 authorize URL을 발급하고 브라우저 로그인 후 callback `code`를 획득한다.
- 실제 검증 단계는 `AUTH_REAL_E2E_CODE=<callback-code>`(네이버는 `AUTH_REAL_E2E_STATE` 포함)로 재실행한다.
- 로컬 `k6`가 없으면 Docker(`grafana/k6`) fallback으로 자동 실행됩니다.
- 웹 대시보드 포함 실행: `make test-k6-dashboard` (기본 URL: `http://127.0.0.1:5665`)

---

## 검증/운영 포인트

- API 스크립트 실행 리포트 기본 경로: `.codex/tmp/ticket-core-service/api-test/latest.md`
- auth-social 파이프라인 리포트 기본 경로: `.codex/tmp/ticket-core-service/api-test/auth-social-e2e-latest.md`
- auth-social real provider e2e 리포트 기본 경로: `.codex/tmp/ticket-core-service/api-test/auth-social-real-provider-e2e-latest.md`
- k6 실행 리포트 기본 경로: `.codex/tmp/ticket-core-service/k6/latest/k6-latest.md`
- 분산/단일 공통 compose 경로: `docker-compose.yml`
- 실시간 푸시 모드 스위치: `APP_PUSH_MODE=sse|websocket` (기본값 `websocket`)
- WS broker 모드 스위치: `APP_WS_BROKER_MODE=simple|relay` (기본값 `simple`)
- compose 기본값:
  - `APP_WS_BROKER_MODE=simple`
  - app 인스턴스 수는 `--scale app=<N>`으로 조절
  - 다중 인스턴스 fanout은 Kafka push topic(`app.kafka.topic.push`) 기반으로 처리
  - relay 모드를 사용할 경우 별도 STOMP relay 브로커를 외부에서 제공해야 함(기본 미사용)
- Kafka push fanout 설정:
  - `APP_KAFKA_TOPIC_PUSH` (기본 `ticket-push-events`)
  - `APP_KAFKA_PUSH_CONSUMER_GROUP_ID` (기본 `${spring.application.name}-${random.uuid}`; 인스턴스별 고유 group)
- 운영 오버라이드 가능한 핵심 설정:
  - `APP_RESERVATION_SOFT_LOCK_TTL_SECONDS` (기본 `30`)
  - `APP_PAYMENT_PROVIDER` (기본 `wallet`)
- WebSocket STOMP 엔드포인트: `/ws` (`/topic/waiting-queue/{concertId}/{userId}`, `/topic/reservations/{seatId}/{userId}`)
- WebSocket 구독 등록 API:
  - `POST /api/push/websocket/waiting-queue/subscriptions`
  - `POST /api/push/websocket/reservations/subscriptions`
- 지갑/결제 API:
  - `GET /api/users/{userId}/wallet`
  - `POST /api/users/{userId}/wallet/charges`
  - `GET /api/users/{userId}/wallet/transactions`
- 예약 결제 연동:
  - `v6/v7 confirm`에서 기본 티켓 금액(`app.payment.default-ticket-price-amount`) 차감
  - `v6/v7 refund`에서 해당 예약 결제 금액 환불
- 인증 세션 무효화:
  - `POST /api/auth/logout` 호출 시 `Authorization: Bearer <accessToken>` + body `refreshToken` 동시 전달 필요
  - 로그아웃 처리 시 refresh 토큰 즉시 revoke + access 토큰은 만료 시각까지 denylist 처리
- 인증 오류 운영 집계:
  - 인증 실패 응답은 `errorCode` 필드를 포함(`AUTH_*`)
  - 운영 로그 집계 키워드: `AUTH_MONITOR code=<AUTH_...>`

---

## 대표 문서 링크

- Service README (Pages Mirror): [AKI AgentOps Pages](https://rag-cargoo.github.io/aki-agentops/#/prj-docs/projects/ticket-core-service/product-docs/README.md)
- API Specs Index (Pages Mirror): [API Specs](https://rag-cargoo.github.io/aki-agentops/#/prj-docs/projects/ticket-core-service/product-docs/api-specs/README.md)
- API Test Guide (Pages Mirror): [API Test Guide](https://rag-cargoo.github.io/aki-agentops/#/prj-docs/projects/ticket-core-service/product-docs/api-test/README.md)
- Sidecar Task (운영 추적): [Task Dashboard](https://github.com/rag-cargoo/aki-agentops/blob/main/prj-docs/projects/ticket-core-service/task.md)
- Sidecar Meeting Notes (운영 기록): [Meeting Notes](https://github.com/rag-cargoo/aki-agentops/blob/main/prj-docs/projects/ticket-core-service/meeting-notes/README.md)

참고: 제품 레포에서는 `prj-docs`를 더 이상 운영 SoT로 사용하지 않으며, 문서 거버넌스는 `AKI AgentOps` sidecar에서 관리합니다.
