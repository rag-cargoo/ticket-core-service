#  모듈 경계 규칙 (Modular Architecture Rules)

##  [CRITICAL] 모듈 간 참조 제한 (Dependency Constraints)

이 프로젝트는 향후 **마이크로서비스(MSA) 분리**를 전제로 한 **모듈러 모놀리스(Modular Monolith)** 구조를 따릅니다.
따라서, 각 도메인(모듈)은 **물리적으로 분리된 서버**라고 가정하고 코딩해야 합니다.

### 1. 절대 금지 사항 (Strict Prohibitions)

1. **Repository 직접 접근 금지 **
    * `TicketService`에서 `UserRepository`를 직접 `Autowired` 하거나 호출해서는 안 됩니다.
    * 다른 도메인의 데이터가 필요하면 반드시 **Service 계층**을 통해서만 접근해야 합니다.
    * *Why?*: 나중에 DB가 쪼개지면 Repository 호출 코드는 다 에러가 나기 때문입니다.

2. **도메인 객체(Entity) 직접 공유 금지 **
    * 다른 모듈의 `Entity`를 자신의 로직 안에서 수정(Setter)하면 안 됩니다.
    * *Why?*: 트랜잭션 범위가 섞여서 분리가 불가능해집니다. ID값(Long)으로만 참조하십시오.

3. **순환 참조 금지 **
    * A 모듈 <-> B 모듈이 서로를 참조하면 안 됩니다. 양방향 의존성은 분리를 불가능하게 만듭니다.

### 2. 허용되는 통신 패턴 (Allowed Patterns)

*  **Service to Service**: `TicketService` -> `UserService.getUserById(id)`
*  **ID Reference**: `Ticket` 엔티티는 `User` 객체 대신 `userId (Long)`만 가짐. (느슨한 결합)
*  **Event Driven**: `MemberJoinedEvent` 발생 시 `CouponService`가 수신 (비동기 처리 권장)

### 3. 레이어 및 패키지 구조 (Layered Package Boundaries)

프로젝트는 명확한 역할 분담을 위해 아래의 3단 레이어 구조를 엄격히 준수한다.

*   **`com.ticketrush.api`**  (Interface Layer)
    *   `controller`: 외부 요청 진입점.
    *   `dto`: 모든 Request/Response 객체 집결. (**Java Class + Lombok 스타일 적용**)
*   **`com.ticketrush.domain`**  (Core Domain Layer)
    *   핵심 비즈니스 로직, 엔티티(Entity), 리포지토리(Repository) 집결.
    *   도메인 간 참조는 반드시 Service를 통해서만 수행한다.
*   **`com.ticketrush.global`**  (Technical Infrastructure Layer)
    *   Kafka, Redis, SSE, Lock 등 공통 기술 지원 컴포넌트 집결.

---

**미래의 AI(또는 개발자)여, 이 규칙을 어기는 순간 MSA 전환의 꿈은 물거품이 된다. 특히 DTO는 반드시 api.dto 패키지에 위치시켜야 함을 명심하라.**
