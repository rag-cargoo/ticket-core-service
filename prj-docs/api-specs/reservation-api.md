# 🎫 Reservation API Specification & Integration Guide

이 문서는 선착순 티켓 예매 시스템의 예약 프로세스와 연동 규격을 정의합니다. 모든 API는 JSON 형식을 기본으로 합니다.

---

## 🛠️ 1. API 상세 명세 (Endpoint Details)

### 1.1. 비동기 예약 요청 (v4, v5)
- **Endpoint**: `POST /api/reservations/{version-strategy}`
- **Description**: 예약 요청을 대기열(Kafka)에 등록하고 즉시 응답을 받습니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `version-strategy` | String | Yes | `v4-opt`, `v4-pes`, `v5-opt` 중 선택 |
| Body | `userId` | Long | Yes | 예매를 시도하는 유저 ID |
| Body | `seatId` | Long | Yes | 예매 대상 좌석 ID |

**Request Example**

```json
{
  "userId": 1,
  "seatId": 10
}
```

**Response Summary (202 Accepted)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `message` | String | 요청 접수 메시지 |
| `strategy` | String | 적용된 동시성 제어 전략 (OPTIMISTIC / PESSIMISTIC) |

**Response Example**

```json
{
  "message": "Reservation request enqueued",
  "strategy": "OPTIMISTIC"
}
```

---

### 1.2. 비동기 예약 상태 조회 (Polling)
- **Endpoint**: `GET /api/reservations/v4/status`
- **Description**: 대기열에 등록된 예약 요청의 현재 처리 상태를 확인합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `userId` | Long | Yes | 요청 유저 ID |
| Query | `seatId` | Long | Yes | 요청 좌석 ID |

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | String | 처리 상태 (`PENDING`, `PROCESSING`, `SUCCESS`, `FAIL_*`) |

**Response Example**

```json
{
  "status": "SUCCESS"
}
```

---

### 1.3. 실시간 알림 구독 (SSE)
- **Endpoint**: `GET /api/reservations/v5/subscribe`
- **Description**: 서버로부터 비동기 처리 결과를 실시간으로 푸시 받기 위한 연결을 수립합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `userId` | Long | Yes | 요청 유저 ID |
| Query | `seatId` | Long | Yes | 요청 좌석 ID |

**Response Summary (200 OK / Event Stream)**

- **Header**: `Content-Type: text/event-stream`
- **Event: `INIT`**: 연결 성공 시 전송 (`data: Connected...`)
- **Event: `RESERVATION_STATUS`**: 최종 처리 결과 전송 (`data: SUCCESS` or `FAIL_*`)

---

### 1.4. 동기식 즉시 예약 (v1, v2, v3)
- **Endpoint**: `POST /api/reservations/{version}`
- **Description**: 대기열 없이 즉시 DB 반영을 시도하는 블로킹 방식입니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `version` | String | Yes | `v1/optimistic`, `v2/pessimistic`, `v3/distributed-lock` 중 선택 |
| Body | `userId` | Long | Yes | 유저 ID |
| Body | `seatId` | Long | Yes | 좌석 ID |

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 생성된 예약 고유 ID |
| `userId` | Long | 예매 유저 ID |
| `seatId` | Long | 예매 좌석 ID |
| `reservationTime` | DateTime | 예약 확정 일시 |

**Response Example**

```json
{
  "id": 7,
  "userId": 1,
  "seatId": 10,
  "reservationTime": "2026-02-05T21:04:19"
}
```

---

### 1.5. 유저별 예약 목록 조회
- **Endpoint**: `GET /api/reservations/users/{userId}`
- **Description**: 특정 유저가 성공한 모든 예약 내역을 조회합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `userId` | Long | Yes | 조회를 원하는 유저 ID |

**Response Example**

```json
[
  {
    "id": 7,
    "userId": 1,
    "seatId": 10,
    "reservationTime": "2026-02-05T21:04:19"
  }
]
```

---

### 1.6. 예약 취소 (Cleanup)
- **Endpoint**: `DELETE /api/reservations/{id}`
- **Description**: 확정된 예약을 취소하고 좌석을 다시 예매 가능 상태로 되돌립니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `id` | Long | Yes | 취소할 예약 레코드 ID |

**Response Summary (204 No Content)**

- 성공 시 응답 바디 없음.

---

## 🚨 2. 공통 에러 응답 (Common Error)
모든 에러 상황(4xx, 5xx)에서 반환되는 표준 객체입니다.

```json
{
  "timestamp": "2026-02-05T21:30:00.000",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/reservations/..."
}
```