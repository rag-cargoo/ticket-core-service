# Ticket Core Service README

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-09 00:33:02`
> - **Updated At**: `2026-02-17 22:17:25`
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
docker-compose up -d
```

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

- 로컬 `k6`가 없으면 Docker(`grafana/k6`) fallback으로 자동 실행됩니다.
- 웹 대시보드 포함 실행: `make test-k6-dashboard` (기본 URL: `http://127.0.0.1:5665`)

---

## 검증/운영 포인트

- API 스크립트 실행 리포트 기본 경로: `.codex/tmp/ticket-core-service/api-test/latest.md`
- k6 실행 리포트 기본 경로: `.codex/tmp/ticket-core-service/k6/latest/k6-latest.md`

---

## 대표 문서 링크

- Service README (Pages Mirror): [AKI AgentOps Pages](https://rag-cargoo.github.io/aki-agentops/#/prj-docs/projects/ticket-core-service/product-docs/README.md)
- API Specs Index (Pages Mirror): [API Specs](https://rag-cargoo.github.io/aki-agentops/#/prj-docs/projects/ticket-core-service/product-docs/api-specs/README.md)
- API Test Guide (Pages Mirror): [API Test Guide](https://rag-cargoo.github.io/aki-agentops/#/prj-docs/projects/ticket-core-service/product-docs/api-test/README.md)
- Sidecar Task (운영 추적): [Task Dashboard](https://github.com/rag-cargoo/aki-agentops/blob/main/prj-docs/projects/ticket-core-service/task.md)
- Sidecar Meeting Notes (운영 기록): [Meeting Notes](https://github.com/rag-cargoo/aki-agentops/blob/main/prj-docs/projects/ticket-core-service/meeting-notes/README.md)

참고: 제품 레포에서는 `prj-docs`를 더 이상 운영 SoT로 사용하지 않으며, 문서 거버넌스는 `AKI AgentOps` sidecar에서 관리합니다.
