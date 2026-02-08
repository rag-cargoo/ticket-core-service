# Ticket Core Service README

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-09 00:33:02`
> - **Updated At**: `2026-02-09 01:11:55`
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

---

## 검증/운영 포인트

- API 스크립트 가이드: [API Script Guide](/workspace/apps/backend/ticket-core-service/prj-docs/api-test/README.md)
- 현재 작업 현황: [Project Task Dashboard](/workspace/apps/backend/ticket-core-service/prj-docs/task.md)
- 기술 로드맵: [Roadmap](/workspace/apps/backend/ticket-core-service/prj-docs/ROADMAP.md)

---

## 대표 문서 링크

- 프로젝트 에이전트 규칙: [Project Agent (Rules)](/workspace/apps/backend/ticket-core-service/prj-docs/PROJECT_AGENT.md)
- 아키텍처: [Architecture](/workspace/apps/backend/ticket-core-service/prj-docs/rules/architecture.md)
- 동시성 전략 기록: [동시성 제어 전략](/workspace/apps/backend/ticket-core-service/prj-docs/knowledge/동시성-제어-전략.md)
- 예약 API 명세: [Reservation API](/workspace/apps/backend/ticket-core-service/prj-docs/api-specs/reservation-api.md)
