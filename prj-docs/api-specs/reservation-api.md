# 🎫 Reservation API Specification & Integration Guide

이 문서는 선착순 티켓 예매 시스템의 예약 프로세스와 연동 규격을 정의합니다. 모든 API는 JSON 형식을 기본으로 합니다.

---

## 🔒 0. 보안 및 진입 정책 (Security Policy)

Step 6 유입량 제어 전략에 따라, 모든 예약 관련 API(`v1` ~ `v4`) 호출 시 아래 정책이 강제됩니다.

*   **필수 헤더**: `User-Id` (Long) - 대기열을 통과한 유저 식별자.
*   **검증 메커니즘**: 서버 인터셉터에서 Redis 내 `active-user:{userId}` 토큰 존재 여부를 확인합니다.
*   **미인증 처리**: 토큰이 없거나 만료된 경우 `403 Forbidden` 에러를 반환합니다.

---

## 🛠️ 1. API 상세 명세 (Endpoint Details)

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
| `RANK_UPDATE` | JSON | 순번 변화 시 전송 (`{"rank": 5, "status": "WAITING"}`) |
| `RESERVATION_STATUS` | String | 최종 예약 결과 (`SUCCESS` / `FAIL`) |

**Response Example**

```text
event: INIT
data: Connected for Queue: 1

event: RANK_UPDATE
data: {"rank": 5, "status": "WAITING"}

event: RESERVATION_STATUS
data: SUCCESS
```

---

### 1.4. 동기식 즉시 예약 (v1, v2, v3)
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