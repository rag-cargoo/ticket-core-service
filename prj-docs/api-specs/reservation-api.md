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
- **URL**: `POST /api/reservations/v4-opt/queue-polling`
- **Method**: `POST`
- **Description**: 예매 요청을 대기열에 등록합니다. (낙관적 락 전략 사용)

**Request Body**
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `userId` | Long | Yes | 예매를 시도하는 유저의 고유 ID |
| `seatId` | Long | Yes | 예매 대상 좌석의 고유 ID |

```json
{
  "userId": 1,
  "seatId": 10
}
```

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
- **Description**: 대기열에 등록된 요청의 현재 처리 상태를 조회합니다.

**Query Parameters**
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `userId` | Long | Yes | 요청 시 사용한 유저 ID |
| `seatId` | Long | Yes | 요청 시 사용한 좌석 ID |

**Response (200 OK)**
```json
{
  "status": "PENDING" 
}
```

**Status 값 가이드**
- `PENDING`: 대기열 진입 완료.
- `PROCESSING`: DB 작업 수행 중.
- `SUCCESS`: **예약 확정.** (완료 화면으로 이동)
- `FAIL`: **예약 실패.** (이미 선택된 좌석 등)
- `NOT_FOUND`: 요청 정보가 유효하지 않음.

---

### 2.3. [SSE] 실시간 알림 구독
- **URL**: `/api/reservations/v5/subscribe`
- **Method**: `GET`
- **Description**: 비동기 처리 결과를 실시간으로 푸시 받기 위해 연결을 유지합니다.

**Query Parameters**
- `userId` (Long), `seatId` (Long) 필수.

**Events**
- `INIT`: 연결 직후 `"Connected for Seat: {id}"` 데이터 전송.
- `RESERVATION_STATUS`: 최종 결과 전송. (Data: `"SUCCESS"` or `"FAIL"`)

---

## 🔒 3. 동기식 예약 (Legacy/Direct)
즉시 결과를 반환받는 방식입니다. 

- **URL**: `POST /api/reservations/v3/distributed-lock`
- **Response (200 OK)**
```json
{
  "id": 7,
  "userId": 1,
  "seatId": 10,
  "reservationTime": "2026-02-05T21:04:19"
}
```

**응답 필드 설명**
- `id`: 생성된 예약 레코드의 고유 ID.
- `userId`: 예매 유저 ID.
- `seatId`: 예매 좌석 ID.
- `reservationTime`: 예약이 확정된 시간 (ISO 8601).
