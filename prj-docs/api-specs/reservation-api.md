# Reservation API Specification & Integration Guide

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-11 22:08:00`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 0. 보안 및 진입 정책 (Security Policy)
> - 1. API 상세 명세 (Endpoint Details)
> - 2. 공통 에러 응답 (Common Error)
<!-- DOC_TOC_END -->

이 문서는 선착순 티켓 예매 시스템의 예약 프로세스와 연동 규격을 정의합니다. 모든 API는 JSON 형식을 기본으로 합니다.

---

## 0. 보안 및 진입 정책 (Security Policy)

Step 6 유입량 제어 전략에 따라, 모든 예약 관련 API(`v1` ~ `v4`) 호출 시 아래 정책이 강제됩니다.

*   **필수 헤더**: `User-Id` (Long) - 대기열을 통과한 유저 식별자.
*   **검증 메커니즘**: 서버 인터셉터에서 Redis 내 `active-user:{userId}` 토큰 존재 여부를 확인합니다.
*   **미인증 처리**: 토큰이 없거나 만료된 경우 `403 Forbidden` 에러를 반환합니다.

---

## 1. API 상세 명세 (Endpoint Details)

### 1.1. 대기열 진입 (Waiting Queue Join)
- **Endpoint**: `POST /api/v1/waiting-queue/join`
- **Description**: 선착순 예매를 위해 대기열에 진입하고 대기 번호를 발급받습니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Body | `userId` | Long | Yes | 예매 시도 유저 고유 ID |
| Body | `concertId` | Long | Yes | 예매 대상 콘서트 ID |

**Request Example**

```json
{
  "userId": 100,
  "concertId": 1
}
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `userId` | Long | 요청 유저 고유 ID |
| `concertId` | Long | 대상 콘서트 ID |
| `status` | String | 현재 상태 (`WAITING`, `ACTIVE`, `REJECTED`) |
| `rank` | Long | 현재 대기 순번 (1부터 시작, ACTIVE 유저는 0) |

**Response Example**

```json
{
  "userId": 100,
  "concertId": 1,
  "status": "WAITING",
  "rank": 42
}
```

---

### 1.2. 비동기 예약 요청 (v4)
- **Endpoint**: `POST /api/reservations/v4/queue`
- **Description**: 예약을 위해 Kafka 대기열에 등록합니다. **반드시 대기열을 통과하여 활성화된 유저여야 합니다.**

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Header | `User-Id` | Long | **Yes** | 인터셉터 검증용 활성 유저 ID |
| Body | `userId` | Long | Yes | 유저 ID (헤더와 일치해야 함) |
| Body | `seatId` | Long | Yes | 좌석 ID |

**Request Example**

```json
// Header: User-Id: 100
{
  "userId": 100,
  "seatId": 1
}
```

**Response Summary (202 Accepted)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `message` | String | 요청 접수 성공 메시지 |
| `strategy` | String | 적용된 동시성 전략 (`OPTIMISTIC` 등) |

**Response Example**

```json
{
  "message": "Reservation request enqueued",
  "strategy": "OPTIMISTIC"
}
```

**Error Case (403 Forbidden)**
대기열을 거치지 않았거나 활성 토큰이 만료된 경우 발생합니다.

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Not an active user in waiting queue",
  "path": "/api/reservations/v4/queue"
}
```

---

### 1.3. 대기열 상태 조회 (Waiting Queue Status)
- **Endpoint**: `GET /api/v1/waiting-queue/status`
- **Description**: 현재 유저의 대기 순번과 상태를 조회합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `userId` | Long | Yes | 유저 고유 ID |
| Query | `concertId` | Long | Yes | 콘서트 고유 ID |

**Request Example**

```bash
GET /api/v1/waiting-queue/status?userId=100&concertId=1
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `userId` | Long | 요청 유저 ID |
| `concertId` | Long | 대상 콘서트 ID |
| `status` | String | 현재 상태 (`WAITING`, `ACTIVE`, `REJECTED`, `NONE`) |
| `rank` | Long | 1부터 시작하는 순번 (ACTIVE 유저는 0) |

**Response Example**

```json
{
  "userId": 100,
  "concertId": 1,
  "status": "WAITING",
  "rank": 5
}
```

---

### 1.4. 실시간 알림 구독 (SSE)
- **Endpoint**: `GET /api/v1/waiting-queue/subscribe`
- **Description**: 서버로부터 비동기 처리 결과 및 대기 순번 변화를 실시간으로 푸시 받습니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `userId` | Long | Yes | 유저 고유 ID |
| Query | `concertId` | Long | Yes | 콘서트 고유 ID |

**Response Summary (200 OK / Event Stream)**

| Event Name | Data Format | Description |
| :--- | :--- | :--- |
| `INIT` | String | 연결 성공 메시지 (`Connected...`) |
| `RANK_UPDATE` | JSON | 순번 변화 시 전송 (`WAITING`/`NONE`) |
| `ACTIVE` | JSON | 활성 전환 시 전송 (`rank=0`, `activeTtlSeconds` 포함) |
| `KEEPALIVE` | JSON | 연결 유지를 위한 heartbeat |
| `RESERVATION_STATUS` | String | 최종 예약 결과 (`SUCCESS` / `FAIL`) |

**Response Example**

```text
event: INIT
data: Connected for Queue: 1

event: RANK_UPDATE
data: {"userId":100,"concertId":1,"status":"WAITING","rank":5,"activeTtlSeconds":0,"timestamp":"2026-02-08T08:20:10.224Z"}

event: ACTIVE
data: {"userId":100,"concertId":1,"status":"ACTIVE","rank":0,"activeTtlSeconds":297,"timestamp":"2026-02-08T08:20:20.120Z"}

event: KEEPALIVE
data: {"timestamp":"2026-02-08T08:20:25.000Z"}

event: RESERVATION_STATUS
data: SUCCESS
```

---

### 1.5. 동기식 즉시 예약 (v1, v2, v3)
- **Endpoint**: `POST /api/reservations/{version}`
- **Description**: 대기열 없이 즉시 DB 반영을 시도하는 블로킹 방식입니다. **활성 토큰이 필수입니다.**

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Header | `User-Id` | Long | **Yes** | 활성 유저 검증용 ID |
| Path | `version` | String | Yes | `v1/optimistic`, `v2/pessimistic`, `v3/distributed-lock` 중 선택 |
| Body | `userId` | Long | Yes | 유저 ID |
| Body | `seatId` | Long | Yes | 좌석 ID |

**Request Example**

```json
// Header: User-Id: 100
{
  "userId": 100,
  "seatId": 1
}
```

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
  "userId": 100,
  "seatId": 1,
  "reservationTime": "2026-02-05T21:04:19"
}
```

---

### 1.6. 유저별 예약 목록 조회
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

### 1.7. 예약 취소 (Cleanup, Legacy)
- **Endpoint**: `DELETE /api/reservations/{id}`
- **Description**: 확정된 예약을 취소하고 좌석을 다시 예매 가능 상태로 되돌립니다.
- **Deprecation Note**: Step 10 이후 신규 플로우는 `POST /api/reservations/v6/{reservationId}/cancel` + `POST /api/reservations/v6/{reservationId}/refund` 사용을 권장합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `id` | Long | Yes | 취소할 예약 레코드 ID |

**Response Summary (204 No Content)**

- 성공 시 응답 바디 없음.

---

### 1.8. Step 9: 좌석 홀드 생성 (HOLD)
- **Endpoint**: `POST /api/reservations/v6/holds`
- **Description**: 결제 대기 상태를 만들고 좌석을 임시 점유(`TEMP_RESERVED`)로 전환합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Body | `userId` | Long | Yes | 예약 요청 유저 ID |
| Body | `seatId` | Long | Yes | 점유할 좌석 ID |

**Response Summary (201 Created)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 예약 ID |
| `status` | String | `HOLD` |
| `holdExpiresAt` | DateTime | 점유 만료 시각 |

---

### 1.9. Step 9: 결제 진행 전이 (HOLD -> PAYING)
- **Endpoint**: `POST /api/reservations/v6/{reservationId}/paying`
- **Description**: 결제 진행 상태로 전이합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `reservationId` | Long | Yes | 대상 예약 ID |
| Body | `userId` | Long | Yes | 예약 소유자 유저 ID |

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | String | `PAYING` |

---

### 1.10. Step 9: 결제 확정 전이 (PAYING -> CONFIRMED)
- **Endpoint**: `POST /api/reservations/v6/{reservationId}/confirm`
- **Description**: 결제 완료를 반영하고 좌석을 최종 점유(`RESERVED`)로 확정합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `reservationId` | Long | Yes | 대상 예약 ID |
| Body | `userId` | Long | Yes | 예약 소유자 유저 ID |

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | String | `CONFIRMED` |
| `confirmedAt` | DateTime | 최종 확정 시각 |

---

### 1.11. Step 9: 예약 상태 조회
- **Endpoint**: `GET /api/reservations/v6/{reservationId}?userId={userId}`
- **Description**: 상태머신 진행 상태(`HOLD/PAYING/CONFIRMED/EXPIRED/CANCELLED/REFUNDED`)와 타임스탬프를 조회합니다.

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | String | 예약 상태 |
| `holdExpiresAt` | DateTime | 홀드 만료 시각 |
| `confirmedAt` | DateTime | 확정 시각 |
| `expiredAt` | DateTime | 만료 시각 |
| `cancelledAt` | DateTime | 취소 시각 |
| `refundedAt` | DateTime | 환불 완료 시각 |

---

### 1.12. Step 10: 예약 취소 + 재판매 대기열 연계 (CONFIRMED -> CANCELLED)
- **Endpoint**: `POST /api/reservations/v6/{reservationId}/cancel`
- **Description**: 확정 예약을 취소하고 좌석을 `AVAILABLE`로 복구한 뒤, 같은 콘서트 대기열 상위 1명을 `ACTIVE`로 승격합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `reservationId` | Long | Yes | 취소 대상 예약 ID |
| Body | `userId` | Long | Yes | 예약 소유자 유저 ID |

**Request Example**

```json
{
  "userId": 1
}
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | String | `CANCELLED` |
| `cancelledAt` | DateTime | 취소 시각 |
| `resaleActivatedUserIds` | Long[] | 재판매로 활성화된 대기열 유저 ID 목록 |

**Response Example**

```json
{
  "id": 10,
  "userId": 1,
  "seatId": 22,
  "status": "CANCELLED",
  "cancelledAt": "2026-02-11T22:10:15",
  "resaleActivatedUserIds": [1002]
}
```

---

### 1.13. Step 10: 환불 완료 처리 (CANCELLED -> REFUNDED)
- **Endpoint**: `POST /api/reservations/v6/{reservationId}/refund`
- **Description**: 취소된 예약에 대해 환불 완료 상태를 기록합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `reservationId` | Long | Yes | 환불 대상 예약 ID |
| Body | `userId` | Long | Yes | 예약 소유자 유저 ID |

**Request Example**

```json
{
  "userId": 1
}
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | String | `REFUNDED` |
| `refundedAt` | DateTime | 환불 완료 시각 |

**Response Example**

```json
{
  "id": 10,
  "userId": 1,
  "seatId": 22,
  "status": "REFUNDED",
  "refundedAt": "2026-02-11T22:11:01"
}
```

---

## 2. 공통 에러 응답 (Common Error)
현재 구현(`GlobalExceptionHandler`)은 대부분 에러를 **문자열 본문**으로 반환합니다.

| HTTP Status | Trigger | Response Body Shape | Example |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `IllegalArgumentException` | Plain text | `User not found: 999` |
| `409 Conflict` | `IllegalStateException` | Plain text | `Only CONFIRMED reservation can transition to CANCELLED.` |
| `500 Internal Server Error` | 기타 예외 | Plain text | `...` |
| `400 Bad Request` | JSON 파싱 실패 | Plain text (prefix 포함) | `JSON Parsing Error: ...` |

```text
Only CANCELLED reservation can transition to REFUNDED.
```
