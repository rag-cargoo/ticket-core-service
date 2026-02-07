# <span style="color: #FFFFFF;">Project Task Dashboard - Ticket Core Service</span>

> 시스템의 현재 상태와 단계별 목표, 세부 완료 내역을 추적하는 통합 보드입니다.

---

## <span style="color: #00D4FF;"> 현재 상태 (Status)</span>
---
> [!NOTE]
>   - **현재 단계**: 동시성 제어 전략 구현 및 검증 (Step 6 완료)
>   - **목표**: 고성능 선착순 티켓팅 시스템 구현
>   - **Tech Stack**: Java 17 / Spring Boot 3.4.1 / JPA / Redisson / PostgreSQL / Redis

---

## <span style="color: #08FFC8;"> 개발 원칙 (Dev Principles)</span>
---
> [!TIP]
>   - **기술 비교/검증**: API 버전을 분리하여 관리 (v1~v4).
>   - **성능 측정**: 각 단계별 부하 테스트 결과를 기록하여 의사결정 근거로 활용.
>   - **문서화 필수**: 실험 결과와 의사결정 과정은 prj-docs/knowledge/에 상세히 기록.
>   - **안전 우선**: 파일 수정 전 원본 확인 및 파괴적 변경 시 사용자 보고 의무화.

---

## <span style="color: #FFFFFF;"> 당면 과제 (Current Tasks)</span>
---
> [!NOTE]
>   - [x] Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
>   - [x] Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
>   - [x] Step 3: Redis 분산 락(Redisson) 구현 및 검증
>   - [x] Step 4: Kafka 기반 비동기 대기열(Waiting Queue) 구현 및 검증
>   - [x] Step 5: Redis Sorted Set 기반 실시간 대기 순번 시스템 구현
>   - [x] Step 6: 대기열 진입 제한(Throttling) 및 유입량 제어 전략 구현
>   - [ ] **Step 7: SSE 기반 실시간 순번 자동 푸시 시스템 (In Progress)**

---

## <span style="color: #00D4FF;"> 진행된 세부 작업 (Completed Details)</span>
---
> ### 1. 동시성 제어 실험 (Concurrency Challenge)
> ---
>   - **Step 0 (락 없음)**: Race Condition 발생 확인 (30명 중 10명 중복 예약).
>   - **Step 1 (낙관적 락)**: JPA @Version을 통한 충돌 감지 및 정합성 보장 확인.
>   - **Step 2 (비관적 락)**: SELECT ... FOR UPDATE를 통한 순차 처리 및 정합성 보장 확인.
>
> ### 2. API Layer Implementation
> ---
>   - **Concert API**: 목록 조회, 옵션 조회, 예약 가능 좌석 조회 엔드포인트 구현.
>   - **Reservation API**: 예약 요청 처리, 비관적 락 로직 및 일반 예약 구현.
>   - **DTOs**: ConcertResponse, SeatResponse, ReservationRequest 등 규격화.
>
> ### 3. Core Domain & DB Layer
> ---
>   - **Domain**: Concert, Seat, Reservation, User 엔티티 및 Repository 구현.
>   - **Infrastructure**: JPA 설정 및 H2/PostgreSQL 연동 확인.
>   - **Data Init**: K-POP 콘서트 더미 데이터 초기화 로직 구현.
>
> ### 4. 프로젝트 아키텍처 정교화 및 인프라 안정화
> ---
>   - **레이어 분리**: api, domain, global 계층 구조 확립.
>   - **DTO 정규화**: 모든 Java record를 class + Lombok 스타일로 전환하여 유연성 확보.
>   - **인프라 안정화**: docker-compose.yml 헬스체크 및 의존성 최적화.