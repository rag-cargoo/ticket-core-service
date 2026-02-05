# 🎫 Reservation API Specification & Integration Guide

이 문서는 프론트엔드 및 클라이언트 작업자를 위한 **티켓 예매 API 연동 가이드**입니다. 

---

## 🛠️ 1. 공통 사항 (Common)

### 공통 에러 응답 포맷 (Error Response)
에러 발생 시(4xx, 5xx) 서버는 아래와 같은 표준 JSON 객체를 반환합니다.
```json
{
  "timestamp": "2026-02-05T21:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/reservations/v4/status"
}
```

---

## 🛠️ 2. API 상세 명세 (Endpoint Details)

### 2.1. 비동기 예약 요청 (Entry)
사용자의 요청을 대기열에 등록합니다. 전략에 따라 3가지 엔드포인트를 제공합니다.

- **URL**: 
    - `POST /api/reservations/v4-opt/queue-polling`: 대기열 + 낙관적 락
    - `POST /api/reservations/v4-pes/queue-polling`: 대기열 + 비관적 락
    - `POST /api/reservations/v5-opt/queue-sse`: 대기열 + SSE 알림 전용
- **Method**: `POST`

**Request Body**
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `userId` | Long | Yes | 예매를 시도하는 유저의 고유 ID |
| `seatId` | Long | Yes | 예매 대상 좌석의 고유 ID |

**Response (202 Accepted)**
```json
{
  "message": "Reservation request enqueued",
  "strategy": "OPTIMISTIC" 
}
```

---

### 2.2. [Polling] 예약 상태 조회
- **URL**: `/api/reservations/v4/status`
- **Method**: `GET`

**Query Parameters**
- `userId` (Long), `seatId` (Long) 필수.

**Response (200 OK)**
```json
{
  "status": "PENDING" 
}
```
*Status 값: `PENDING`, `PROCESSING`, `SUCCESS`, `FAIL`, `FAIL_OPTIMISTIC_CONFLICT`, `NOT_FOUND`*

---

### 2.3. [SSE] 실시간 알림 구독
- **URL**: `/api/reservations/v5/subscribe`
- **Method**: `GET`

**Query Parameters**
- `userId` (Long), `seatId` (Long) 필수.

**Events**
- `INIT`: 연결 직후 전송.
- `RESERVATION_STATUS`: 최종 결과 전송 (`SUCCESS` or `FAIL`).

---

## 🔒 3. 동기식 예약 (Legacy/Direct)
즉시 결과를 반환받는 블로킹 방식입니다.

- **v1**: `POST /api/reservations/v1/optimistic`
- **v2**: `POST /api/reservations/v2/pessimistic`
- **v3**: `POST /api/reservations/v3/distributed-lock`

**Response (200 OK)**
```json
{
  "id": 1,
  "userId": 1,
  "seatId": 10,
  "reservationTime": "2026-02-05T21:04:19"
}
```

---

## 📖 4. 내 예약 관리

### 4.1. 유저별 예약 목록 조회
- **URL**: `GET /api/reservations/users/{userId}`
- **Response (200 OK)**: `ReservationResponse` 배열 반환.

### 4.2. 예약 취소
- **URL**: `DELETE /api/reservations/{id}`
- **Response**: `204 No Content`