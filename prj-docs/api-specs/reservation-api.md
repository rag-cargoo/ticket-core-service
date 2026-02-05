# Reservation API Specification

티켓 예매 및 내역 조회, 취소를 담당하는 API입니다. 동시성 제어 전략에 따라 3가지 버전의 생성 API를 제공합니다.

## 1. 티켓 예매 (Create)
- **Endpoints**:
    - `POST /api/reservations/v1/optimistic` (낙관적 락)
    - `POST /api/reservations/v2/pessimistic` (비관적 락)
    - `POST /api/reservations/v3/distributed-lock` (분산 락)

### Request Body
```json
{
  "userId": 1,
  "seatId": 1
}
```

### Response Body (200 OK)
```json
{
  "id": 1,
  "userId": 1,
  "seatId": 1,
  "reservationTime": "2026-02-05T16:51:54"
}
```

---

## 2. 내 예약 내역 조회
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
