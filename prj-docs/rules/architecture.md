# 모듈 경계 규칙 (Modular Architecture Rules)

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-16 19:20:31`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - [CRITICAL] 모듈 간 참조 제한 (Dependency Constraints)
> - 2. 허용되는 통신 패턴 (Allowed Patterns)
> - 3. 레이어 및 패키지 구조 (Layered Package Boundaries)
> - 4. Reservation 경계 Port/Adapter 규칙 (R2)
<!-- DOC_TOC_END -->

> **Purpose**: MSA 분리 가능성을 유지하기 위해 모듈 간 의존 경계를 강제합니다.

---

## [CRITICAL] 모듈 간 참조 제한 (Dependency Constraints)
---
> [!WARNING]
> 이 프로젝트는 향후 **마이크로서비스(MSA) 분리**를 전제로 한 **모듈러 모놀리스(Modular Monolith)** 구조를 따릅니다.
> 따라서, 각 도메인(모듈)은 **물리적으로 분리된 서버**라고 가정하고 코딩해야 합니다.
>
> ### 1. 절대 금지 사항 (Strict Prohibitions)
>
> 1. **Repository 직접 접근 금지**
>    - `TicketService`에서 `UserRepository`를 직접 `Autowired` 하거나 호출해서는 안 됩니다.
>    - 다른 도메인의 데이터가 필요하면 반드시 **Service 계층**을 통해서만 접근해야 합니다.
>    - *Why?*: 나중에 DB가 쪼개지면 Repository 호출 코드는 즉시 장애 포인트가 됩니다.
>
> 2. **도메인 객체(Entity) 직접 공유 금지**
>    - 다른 모듈의 `Entity`를 자신의 로직 안에서 수정(Setter)하면 안 됩니다.
>    - *Why?*: 트랜잭션 범위가 섞여서 분리가 불가능해집니다. ID값(Long)으로만 참조하십시오.
>
> 3. **순환 참조 금지**
>    - A 모듈 <-> B 모듈이 서로를 참조하면 안 됩니다. 양방향 의존성은 분리를 불가능하게 만듭니다.

---

## 2. 허용되는 통신 패턴 (Allowed Patterns)
---
> [!TIP]
> 허용 패턴은 "서비스 경계 명확화"와 "느슨한 결합"을 동시에 만족해야 합니다.
>
> - **Service to Service**: `TicketService` -> `UserService.getUserById(id)`
> - **ID Reference**: `Ticket` 엔티티는 `User` 객체 대신 `userId (Long)`만 가짐 (느슨한 결합)
> - **Event Driven**: `MemberJoinedEvent` 발생 시 `CouponService`가 수신 (비동기 처리 권장)

---

## 3. 레이어 및 패키지 구조 (Layered Package Boundaries)
---
프로젝트는 명확한 역할 분담을 위해 아래의 3단 레이어 구조를 엄격히 준수합니다.

- **`com.ticketrush.api`** (Interface Layer)
  - `controller`: 외부 요청 진입점
  - `dto`: 모든 Request/Response 객체 집결 (**Java Class + Lombok 스타일 적용**)
- **`com.ticketrush.domain`** (Core Domain Layer)
  - 핵심 비즈니스 로직, 엔티티(Entity), 리포지토리(Repository) 집결
  - 도메인 간 참조는 반드시 Service를 통해서만 수행
- **`com.ticketrush.global`** (Technical Infrastructure Layer)
  - Kafka, Redis, SSE, Lock 등 공통 기술 지원 컴포넌트 집결

---

**이 규칙을 어기면 MSA 전환 비용이 급증합니다. 특히 DTO는 반드시 `api.dto` 패키지에 위치시켜야 합니다.**

## 4. Reservation 경계 Port/Adapter 규칙 (R2)
---
> [!TIP]
> Reservation 도메인에서 외부 경계 의존(`User`, `Seat`, `WaitingQueue`)은 Port/Adapter로 명시화한다.
>
> - Port 인터페이스 위치: `domain/reservation/port/outbound/*Port.java`
> - Adapter 구현 위치: `domain/reservation/adapter/outbound/*PortAdapter.java`
> - 서비스 계층(`domain/reservation/service`)은 구현체/외부 서비스 대신 Port 인터페이스만 주입한다.
>
> ### 금지 패턴
>
> - `ReservationServiceImpl`, `ReservationLifecycleServiceImpl`가 `UserRepository`/`SeatRepository`/`WaitingQueueService`를 직접 주입
> - Reservation 서비스에서 외부 경계 구현 클래스를 타입으로 직접 의존
>
> ### 허용 패턴
>
> - `ReservationUserPort`/`ReservationSeatPort`/`ReservationWaitingQueuePort` 인터페이스 주입
> - 구현 세부사항은 `*PortAdapter`에 캡슐화
>
> ### 배치 체크리스트 (R2)
>
> - [ ] Port 인터페이스 추가(동작 불변)
> - [ ] Adapter 구현 추가(기존 컴포넌트 위임)
> - [ ] Reservation 서비스 주입 타입 전환
> - [ ] 통합 테스트 wiring 동기화
> - [ ] `task.md` + 회의록 + 이슈 코멘트 동기화
