# Backend API - 작업 현황 (Ticket Core Service)

## 🚦 현재 상태 (Status)

- **현재 단계**: 동시성 제어 전략 구현 및 검증 (Step 2 완료) ✅
- **목표**: 고성능 선착순 티켓팅 시스템 구현
- **Tech Stack**: Java 17, Spring Boot 3.4.1, JPA, Redisson, H2, PostgreSQL, Redis

## 📐 개발 원칙 (Dev Principles)

- **기술 비교/검증**: API 버전을 분리하여 관리 (`v1_Basic`, `v2_Lock`, `v3_Redis`, `v4_Kafka`).
- **성능 측정**: 각 단계별 부하 테스트(k6) 결과를 기록하여 의사결정 근거로 삼는다.
- **문서화 필수**: 실험 결과와 의사결정 과정은 `prj-docs/knowledge/`에 기록하고 사이드바에 노출한다.
- **안전 우선**: 파일 수정 전 원본을 확인하고 파괴적 변경 시 사용자에게 보고한다.

## ✅ 당면 과제 (Current Tasks)

- [x] Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
- [x] Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
- [x] Step 3: Redis 분산 락(Redisson) 구현 및 검증
- [x] Step 4: Kafka 기반 비동기 대기열(Waiting Queue) 구현 및 검증
- [ ] Step 5: Redis Sorted Set 기반 실시간 대기 순번 시스템 구현 👈 **Next Work**

---

## 🏗️ 진행된 세부 작업 (Completed Details)

### 1. 동시성 제어 실험 (Concurrency Challenge)
- **Step 0 (락 없음)**: Race Condition 발생 확인 (30명 중 10명 중복 예약).
- **Step 1 (낙관적 락)**: JPA `@Version`을 통한 충돌 감지 및 정합성 보장 확인.
- **Step 2 (비관적 락)**: `SELECT ... FOR UPDATE`를 통한 순차 처리 및 강력한 정합성 보장 확인.

### 2. API Layer Implementation
- **Concert API**:
  - `ConcertController`: 콘서트 조회, 옵션 조회, 예약 가능 좌석 조회 엔드포인트 구현.
  - `ConcertService`: 인터페이스 및 구현체 분리 (Service Interface 패턴 적용).
  - `DTOs`: `ConcertResponse`, `ConcertOptionResponse`, `SeatResponse` 생성.
- **Reservation API**:
  - `ReservationController`: 예약 요청 처리 (`POST /reservations`) 구현.
  - `ReservationService`: 비관적 락(`createReservationWithPessimisticLock`) 및 일반 예약 로직 구현.
  - `DTOs`: `ReservationRequest`, `ReservationResponse` 생성.

### 3. Core Domain & DB Layer
- **Concert Domain**: Concert(Artist 추가), ConcertOption, Seat 엔티티 및 Repository 구현.
- **Reservation Domain**: Reservation 엔티티 및 Repository 구현.
- **User Domain**: User 엔티티 및 Repository 구현.
- **Infrastructure**: JPA 설정 및 H2/PostgreSQL 연동 확인.
- **Data Init**: K-POP 콘서트 더미 데이터(IU, NewJeans, BTS) 초기화 로직 구현.

### 4. 프로젝트 아키텍처 정교화 및 인프라 안정화 (Architecture Refinement) ✅
- **레이어 분리**: `interfaces`, `infrastructure`를 폐기하고 `api`, `global`로 개편하여 명확한 3단 계층 구조 확립.
- **DTO 정규화**: 모든 Java `record`를 `class` + Lombok 스타일로 전환하여 유연성 확보 및 일관된 패키지(`api.dto`) 관리.
- **인프라 안정화**: `docker-compose.yml`에 건강 체크(Healthcheck) 및 명시적 의존성(depends_on)을 적용하여 기동 불안정성 원천 차단.
- **거버넌스 수립**: API 명세 표준 템플릿(6단계)을 수립하고 전체 문서 현행화 완료.

### 5. 프로젝트 인프라 및 문서화 (Legacy) ✅
