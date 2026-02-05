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

---

## 2. 비동기 예약 상태 확인

### [Polling] 예약 상태 조회
- **Endpoint**: `GET /api/reservations/v4/status?userId={userId}&seatId={seatId}`
- **Responses**: `PENDING`, `PROCESSING`, `SUCCESS`, `FAIL`, `FAIL_OPTIMISTIC_CONFLICT`

### [SSE] 실시간 구독
- **Endpoint**: `GET /api/reservations/v5/subscribe?userId={userId}&seatId={seatId}`
- **Event Name**: `RESERVATION_STATUS`
- **Data**: `SUCCESS` or `FAIL`

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
