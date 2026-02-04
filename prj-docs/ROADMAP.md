# Backend API - 개발 로드맵

## 🎯 목표: 고성능 선착순 예매 시스템 (Ticket-Rush)

- **핵심 학습 목표**: 대규모 트래픽(Traffic Spike) 대응 및 데이터 정합성 보장
- **Tech Stack**:
  - Core: Java 17, Spring Boot 3.4
  - DB: PostgreSQL (Main), H2 (Local Test)
  - Cache/Lock: **Redis** (캐싱, 분산락, 대기열 관리)
  - Message Queue: **Kafka** (대기열 버퍼링, 트래픽 제어)
  - ORM: JPA, QueryDSL

## 📋 기능 백로그

### 🏛️ 아키텍처 전략 (Architecture Strategy)

현재는 **모듈러 모놀리스(Modular Monolith)** 형태로 시작하여, 티켓팅 코어(Core)부터 단단하게 구축합니다. 추후 트래픽 증가 및 기능 확장에 따라 다음과 같이 분리할 예정입니다.

1. **Ticket Core Service** (현재 진행 중): 티켓, 공연, 결제 등 데이터 정합성이 중요한 영역
2. **Community & Live Service** (Future): 채팅, 스트리밍 등 실시간성이 중요한 영역 (별도 DB/서버)

### Phase 1: 기반 시스템 구축 (Basic)

- [x] 프로젝트 초기 설정 (Spring Boot, Docker)
- [ ] **Infrastructure Setup**: Redis, Kafka 컨테이너 추가 설정
- [ ] User Domain: 사용자 회원가입/로그인 (JWT)
- [ ] Concert/Seat Domain: 공연 및 좌석 정보 관리 (CRUD)

### Phase 1.5: K-POP 엔터테인먼트 구조 확장 (Expansion)

- [ ] **Agency & Artist Domain**: 기획사(YG, SM, HYBE...) 및 아티스트 계층 구조 적용
- [ ] **Data Migration**: 단순 문자열 Artist -> Entity 관계 매핑으로 전환

### Phase 2: 동시성 정복 (Concurrency Challenge)

- [ ] **좌석 예매 API 구현 (재고 차감)**
  - [ ] Scenario A: Java `synchronized` (단일 서버)
  - [ ] Scenario B: DB **Pessimistic Lock** (비관적 락)
  - [ ] Scenario C: DB **Optimistic Lock** (낙관적 락)
  - [ ] Scenario D: Redis **Distributed Lock** (Redisson)
- [ ] **성능 테스트 및 비교 보고서 작성 (k6/JMeter)**

### Phase 3: 대용량 트래픽 대응 (Traffic Spike)

- [ ] **대기열 시스템(Waiting Queue) 구현**
  - [ ] Redis `Sorted Set`을 이용한 대기 순번 발급
  - [ ] Kafka를 이용한 예매 요청 비동기 처리 및 버퍼링
- [ ] 캐싱 전략 적용 (공연 조회 성능 개선)

### Phase 4: 안정성 및 모니터링

- [ ] Circuit Breaker (Resilience4j) 적용
- [ ] Prometheus + Grafana 모니터링 (Optional)

### Phase 5: K-POP 팬덤 플랫폼 확장 (Fan Interactive)

- [ ] **실시간 팬덤 채팅 (Live Chat)**
  - [ ] WebSocket / STOMP 프로토콜 적용
  - [ ] 아티스트별/콘서트별 채팅룸 분리
- [ ] **라이브 스트리밍 채널 (Live Streaming)**
  - [ ] 방송 상태 관리 (ON/OFF) 및 채널링 시스템
  - [ ] 스트리밍 서버 연동 (HLS/RTMP)
