# Kafka 기반 비동기 대기열 설계 전략

> **Purpose**: 대규모 트래픽 발생 시 서버 부하를 제어하고, 안정적인 예약 처리를 위한 비동기 아키텍처 기록
> **Date**: 2026-02-05

## 1. 아키텍처 개요 (Architecture)

단순한 락(Lock) 방식은 트래픽이 몰릴 때 모든 커넥션을 점유하여 시스템 전체를 마비시킵니다. 이를 해결하기 위해 요청을 즉시 처리하지 않고 **Kafka**라는 완충 지대에 담아 서버가 감당 가능한 속도로 처리하는 방식을 도입했습니다.

### 데이터 흐름
1. **Producer**: 사용자의 예약 요청을 `ticket-reservation-events` 토픽으로 전송 (상태: `PENDING`)
2. **Kafka**: 요청 메시지를 순차적으로 저장
3. **Consumer**: 메시지를 꺼내어 실제 DB 예약 로직 수행 (상태: `PROCESSING` -> `SUCCESS/FAIL`)
4. **Notification**: SSE 또는 Polling을 통해 사용자에게 최종 결과 통보

---

## 2. 핵심 설계 결정 (Key Decisions)

### 2.1. 파티셔닝 전략: `seatId`를 메시지 키로 사용
Kafka는 동일 파티션 내에서만 순서를 보장합니다. 선착순 예매에서 가장 중요한 것은 **"동일한 좌석에 대해 누가 먼저 요청했는가"**입니다.
- **결정**: `seatId`를 Kafka 메시지의 **Key**로 설정.
- **이유**: 동일 좌석에 대한 모든 요청이 같은 파티션으로 들어가게 되어, 컨슈머가 이를 물리적으로 순차 처리(Serial Processing)할 수 있게 됨. 정합성 유지의 핵심.

### 2.2. 상태 관리 (State Management)
비동기 환경에서 사용자는 자신의 요청이 어떻게 되었는지 궁금해합니다.
- **도구**: Redis (`StringRedisTemplate`)
- **키 구조**: `reservation:status:{userId}:{seatId}`
- **TTL**: 30분 (불필요한 메모리 점유 방지)

---

## 3. 트러블슈팅: JSON 직렬화 (Serialization)

### 문제 상황
`ReservationEvent` 객체를 Kafka로 전송 시 `ClassCastException` 발생.
- **원인**: 스프링 부트의 기본 설정이 `StringSerializer`로 되어 있어 객체를 문자열로 변환하려다 실패함.

### 해결 방법
`application.yml` 설정을 통해 JSON 기반의 직렬화/역직렬화 체계를 강제함.
```yaml
spring:
  kafka:
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

---

## 4. 향후 과제 (Future Work)
- **Step 5**: 단순 큐를 넘어 Redis `Sorted Set`을 활용하여 사용자에게 **"대기 순번(Rank)"**을 실시간으로 제공하는 기능 추가 예정.
