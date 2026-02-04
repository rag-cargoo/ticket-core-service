# Backend API - 작업 현황

## 🚦 현재 상태 (Status)

- **현재 단계**: 개발 환경 구축 완료 ✅
- **목표**: Spring Boot 기반의 API 서버 구축
- **Tech Stack**: Java 17, Spring Boot 3.4.1, JPA, QueryDSL, H2 (로컬), PostgreSQL (Docker)

## 📐 개발 원칙 (Dev Principles) - [Project Specific]

- **기술 비교/검증**: Kafka/Redis 도입 전후 비교를 위해 **API 버전을 분리**합니다.
  - `v1_Basic` (DB-only), `v2_Lock` (DB Lock), `v3_Redis` (Distributed Lock), `v4_Kafka` (Async Queue)
- **성능 측정**: 각 단계별 부하 테스트(k6) 결과를 기록하여 의사결정 근거로 삼습니다.

## ✅ 당면 과제 (Current Tasks)

- [x] Spring Boot 프로젝트 스캐폴딩
- [x] 서비스 프로파일 분리 (local, docker)
- [x] Dockerfile 및 docker-compose 설정
- [x] DB 설정 개선 (H2 + PostgreSQL)
- [x] 로컬 환경 실행 테스트
- [x] 기본 REST API 구현: Concert (Service, Controller, DTO)
- [x] 기본 REST API 구현: Reservation (예약 생성)
- [ ] 동시성 제어 및 락(Lock) 구현 (Pessimistic, Optimistic)

## 🏗️ 진행된 세부 작업 (Completed Details)

### API Layer Implementation (New)

- **Concert API**:
  - `ConcertController`: 콘서트 조회, 옵션 조회, 예약 가능 좌석 조회 엔드포인트 구현
  - `ConcertService`: 도메인 엔티티 조회 로직 구현
  - `DTOs`: `ConcertResponse`, `ConcertOptionResponse`, `SeatResponse` 생성
- **Reservation API**:
  - `ReservationController`: 예약 요청 처리 (`POST /reservations`) 구현
  - `ReservationService`: 좌석 점유 및 예약 생성 트랜잭션 로직 구현
  - `DTOs`: `ReservationRequest`, `ReservationResponse` 생성

### Core Domain & DB Layer

- [x] **Concert Domain**: Concert(Artist 추가), ConcertOption, Seat 엔티티 및 Repository 구현
- [x] **Reservation Domain**: Reservation 엔티티 및 Repository 구현
- [x] **User Domain**: User 엔티티 및 Repository 구현
- [x] **Infrastructure**: JPA 설정 및 H2/PostgreSQL 연동 확인
- [x] **Data Init**: K-POP 콘서트 더미 데이터(IU, NewJeans, BTS) 초기화 로직 구현
