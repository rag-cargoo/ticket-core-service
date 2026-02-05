# Reservation API Specification

티켓 예매 및 내역 조회, 취소를 담당하는 API입니다. 동시성 제어 전략에 따라 3가지 버전의 생성 API를 제공합니다.

## 1. 티켓 예매 (Create)
- **Endpoints (Synchronous - Blocking)**:
    - `POST /api/reservations/v1/optimistic`: 낙관적 락 기반 즉시 예약
    - `POST /api/reservations/v2/pessimistic`: 비관적 락 기반 순차 예약
    - `POST /api/reservations/v3/distributed-lock`: Redis 분산 락 기반 고성능 예약

- **Endpoints (Asynchronous - Non-blocking)**:
    - `POST /api/reservations/v4-opt/queue-polling`: 대기열 진입 (낙관적 락 전략)
    - `POST /api/reservations/v4-pes/queue-polling`: 대기열 진입 (비관적 락 전략)
    - `POST /api/reservations/v5-opt/queue-sse`: 대기열 진입 + SSE 실시간 결과 알림

### Request Body
```json
{
  "userId": 1,
  "seatId": 1
}
```

### Response (202 Accepted)
비동기 처리가 시작되었음을 의미합니다.
```text
Reservation request enqueued. Strategy: OPTIMISTIC
```

---

## 2. 비동기 예약 상태 확인

### [Polling] 예약 상태 조회
사용자가 명시적으로 결과를 확인할 때 사용합니다.
- **Endpoint**: `GET /api/reservations/v4/status?userId={userId}&seatId={seatId}`

| Status Code | 의미 | 설명 |
| :--- | :--- | :--- |
| **PENDING** | 대기 중 | Kafka 큐에 적재되어 처리를 기다리는 상태 |
| **PROCESSING** | 처리 중 | 컨슈머가 메시지를 읽어 DB 작업을 수행 중인 상태 |
| **SUCCESS** | 성공 | 예약이 완료되어 DB에 반영된 상태 |
| **FAIL** | 실패 | 좌석 이미 선점 등 비즈니스 로직에 의한 실패 |
| **FAIL_OPTIMISTIC_CONFLICT** | 충돌 | 낙관적 락 충돌로 인해 처리에 실패한 상태 |

### [SSE] 실시간 구독
서버가 처리가 끝나는 즉시 클라이언트로 결과를 푸시합니다.
- **Endpoint**: `GET /api/reservations/v5/subscribe?userId={userId}&seatId={seatId}`
- **Events**:
    - `INIT`: 연결 성공 시 전송
    - `RESERVATION_STATUS`: 최종 결과 전송 (`SUCCESS` or `FAIL`)

---

## 3. 내 예약 내역 조회
- **Endpoint**: `GET /api/reservations/users/{userId}`
- **Description**: 특정 유저의 모든 예약 내역을 조회합니다.

### Response Body (200 OK)
```json
[
  {
    "id": 1,
    "userId": 1,
    "seatId": 1,
    "reservationTime": "2026-02-05T16:51:54"
  }
]
```

---

## 3. 예약 취소
- **Endpoint**: `DELETE /api/reservations/{id}`
- **Description**: 예약을 취소하고 좌석을 AVAILABLE 상태로 되돌립니다.

### Response (204 No Content)
- No Body
