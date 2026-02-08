# Waiting Queue API Specification

대기열 진입, 순번 조회, 상태 전환을 위한 API 규격입니다.

---

## 1. 개요 (Overview)
---
> [!NOTE]
> **Base URL**: `/api/v1/waiting-queue`
> **Status**: Active (Step 7 완료, 운영 안정화 단계)
>
> | 기능 | Method | Path | Auth |
> | :--- | :--- | :--- | :--- |
> | 대기열 진입 | POST | `/join` | User |
> | 대기 상태 조회 | GET | `/status` | User |
> | 실시간 순번 구독 | GET | `/subscribe` | User |

---

## 2. API 상세 명세 (Endpoint Details)

### 2.1. 대기열 진입 (Join Queue)
- **Endpoint**: `POST /api/v1/waiting-queue/join`
- **Description**: 사용자를 특정 공연의 대기열에 등록하고 현재 상태/순번을 반환합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Body | `userId` | Long | Yes | 대기열 진입 유저 ID |
| Body | `concertId` | Long | Yes | 대상 콘서트 ID |

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
| `userId` | Long | 유저 ID |
| `concertId` | Long | 콘서트 ID |
| `status` | String | `WAITING`, `ACTIVE`, `REJECTED` |
| `rank` | Long | 대기 순번 (1부터 시작, `ACTIVE`는 0) |

**Response Example**

```json
{
  "userId": 100,
  "concertId": 1,
  "status": "WAITING",
  "rank": 450
}
```

---

### 2.2. 대기 상태 조회 (Get Status)
- **Endpoint**: `GET /api/v1/waiting-queue/status`
- **Description**: 특정 유저의 현재 대기 상태 및 순번을 조회합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `userId` | Long | Yes | 조회 대상 유저 ID |
| Query | `concertId` | Long | Yes | 조회 대상 콘서트 ID |

**Request Example**

```bash
GET /api/v1/waiting-queue/status?userId=100&concertId=1
```

**Response Example (WAITING)**

```json
{
  "status": "WAITING",
  "rank": 120
}
```

**Response Example (ACTIVE)**

```json
{
  "status": "ACTIVE",
  "rank": 0
}
```

---

### 2.3. 실시간 순번 구독 (SSE Subscribe)
- **Endpoint**: `GET /api/v1/waiting-queue/subscribe`
- **Description**: 대기열 상태를 실시간으로 구독합니다. 최초 연결 직후 현재 상태 스냅샷을 전송하며, 스케줄러 활성화 시 순번 업데이트를 푸시합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `userId` | Long | Yes | 구독 유저 ID |
| Query | `concertId` | Long | Yes | 구독 콘서트 ID |

**Response Summary (200 OK / Event Stream)**

| Event Name | Data Format | Description |
| :--- | :--- | :--- |
| `INIT` | String | 연결 확인 메시지 |
| `RANK_UPDATE` | JSON | `WAITING`/`NONE` 상태 갱신 |
| `ACTIVE` | JSON | 활성 전환 이벤트 (`activeTtlSeconds` 포함) |
| `KEEPALIVE` | JSON | 연결 유지를 위한 heartbeat |

**Payload Example (`RANK_UPDATE`)**

```json
{
  "userId": 100,
  "concertId": 1,
  "status": "WAITING",
  "rank": 12,
  "activeTtlSeconds": 0,
  "timestamp": "2026-02-08T08:20:10.224Z"
}
```

**Payload Example (`ACTIVE`)**

```json
{
  "userId": 100,
  "concertId": 1,
  "status": "ACTIVE",
  "rank": 0,
  "activeTtlSeconds": 297,
  "timestamp": "2026-02-08T08:20:20.120Z"
}
```

---

## 3. 에러 코드 (Error Codes)

| Code | Message | Description |
| :--- | :--- | :--- |
| 429 | Too Many Requests | 대기열 진입 시도가 너무 빈번함 |
| 404 | Concert Not Found | 존재하지 않는 공연 ID |

---

## 4. 테스트 케이스 (Test Cases)

1. 사용자가 처음 진입 시 `WAITING` 상태와 0보다 큰 `rank`를 받는다.
2. 시간이 지나면 `rank`가 점진적으로 줄어든다.
3. 순번이 0이 되면 상태가 `ACTIVE`로 변한다.

---

## 5. 비고 (Notes)

- Redis Sorted Set을 사용하여 실시간 순번을 계산합니다.
- `ACTIVE` 상태로 전환된 유저는 5분 내에 예약을 완료해야 합니다.
