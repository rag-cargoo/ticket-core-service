# MSA 데이터 분리 및 동기화 전략 (Data Sync Strategy)

> **Purpose**: MSA 전환 시 데이터 소유권과 동기화 방식의 기준을 명확히 정의합니다.
> **Scope**: Concert/Ticket 도메인 분리 시나리오 중심.

---

## 1. 핵심 원칙 (Core Principle)
---
> [!NOTE]
> **"서비스는 독립적으로 생존해야 한다 (Fault Tolerance)."**
>
> 하나의 마이크로서비스(예: `Concert Service`)가 장애로 중단되더라도, 다른 서비스(예: `Ticket Service`)는 멈추지 않고 핵심 기능을 수행할 수 있어야 합니다.

---

## 2. 데이터 분리 모델 (Data Separation Model)
---
> [!TIP]
> 서비스 간 결합을 줄이기 위해 "소유권 분리 + 읽기 모델 복제"를 기본 원칙으로 삼습니다.

### A. 서비스별 DB 소유권

- **Concert Service**: `Concert`, `ConcertOption`, `Seat` (Master Data)
- **Ticket Service**: `Reservation`, `Payment` (Master Data)

### B. 타 서비스 데이터 참조 전략

`Ticket Service`가 예매 내역을 보여줄 때 `Concert` 정보(제목, 날짜)가 반드시 필요합니다. 이를 위해 **API 호출 대신 데이터 복제** 방식을 지향합니다.

#### 1 단계: 초기 (Modular Monolith) - **Current**

- 물리적으로 같은 DB를 사용하므로 `JOIN`이나 `Service` 호출로 해결.
- 단, 코드 레벨에서는 `concertId`만 참조하도록 강제하여 결합도 낮춤.

#### 2 단계: 분리 후 (Microservices) - **Target**

- **Ticket Service DB** 내부에 `ConcertReplica` 테이블 생성.
- **읽기 전용(Read-Only)** 데이터로 관리하며, `Concert Service`에 장애가 발생해도 예매 조회 가능.

---

## 3. 데이터 동기화 메커니즘 (Synchronization)
---
> [!WARNING]
> 애플리케이션 코드에 동기화 로직을 직접 넣으면 장애와 정합성 이슈가 커집니다. 인프라 레벨 자동화를 우선합니다.

### A. 로컬 / 온프레미스 (CDC)

- **Debezium + Kafka Connect**: DB 로그(Binlog)를 감지하여 자동으로 변경 사항을 전파합니다.
- 개발자의 개입 없이 데이터 일관성을 보장합니다.

### B. 클라우드 (AWS RDS)

- **RDS Read Replica**: AWS 관리형 서비스를 사용하여 클릭 몇 번으로 복제본을 생성합니다.
- **DMS (Database Migration Service)**: 서로 다른 DB 간의 실시간 복제가 필요할 때 사용합니다.

---

## 4. 결론 (Conclusion)
---
- **No Code Sync**: 애플리케이션 코드 내에 동기화 로직을 작성하지 않습니다.
- **Infrastructure as Code**: 데이터 복제는 인프라 설정(Terraform 등)으로 관리합니다.

---
> **Note**: 이 문서는 향후 MSA 전환 시 기술적 의사결정의 기준이 됩니다.
