# 🎫 Reservation API Specification & Integration Guide

이 문서는 **선착순 티켓 예매 시스템**의 예약 프로세스와 API 상세 연동 규격을 정의합니다.

---

## 🎯 1. 기능 개요 및 전략 가이드

### 1.1. 예매 전략 선택 (Usage Policy)
- **실제 서비스 (v4, v5)**: 대규모 접속자가 몰리는 공연에 사용. 요청을 즉시 처리하지 않고 대기열(`Kafka`)에 담아 안정적으로 처리합니다.
- **테스트/소규모 (v1~v3)**: 정합성 테스트용 혹은 트래픽이 적은 환경에서 사용합니다.

### 1.2. 비동기 예약 워크플로우 (v4, v5 Sequence)
1. **[Client]** 예약 요청 (`POST /api/reservations/v4-opt/queue-polling`)
2. **[Server]** `202 Accepted` 반환 및 Kafka 이벤트 발행
3. **[Client]** 상태 확인 (Polling 호출 혹은 SSE 구독)
4. **[Server]** 처리 완료 시 최종 상태(`SUCCESS/FAIL`) 전달

---

## 🛠️ 2. API 상세 명세 (Endpoint Details)

### 2.1. 비동기 예약 요청 (Entry)
사용자를 대기열에 등록합니다.

- **URL**: 
    - `POST /api/reservations/v4-opt/queue-polling` (낙관적 락 전략)
    - `POST /api/reservations/v4-pes/queue-polling` (비관적 락 전략)
    - `POST /api/reservations/v5-opt/queue-sse` (SSE 알림 전용)
- **Method**: `POST`

**Request Body**
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `userId` | Long | Yes | 예매 시도 유저 ID |
| `seatId` | Long | Yes | 대상 좌석 고유 ID |

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

### 2.2. 예약 상태 조회 (Polling)
- **URL**: `GET /api/reservations/v4/status`
- **Query Parameters**: `userId` (Long), `seatId` (Long)

**Response (200 OK)**
```json
{
  "status": "PENDING"
}
```
| status 값 | 의미 | 설명 |
| :--- | :--- | :--- |
| `PENDING` | 대기 중 | 큐에서 처리를 기다림 |
| `PROCESSING` | 처리 중 | DB 작업 진행 중 |
| `SUCCESS` | 성공 | **예약 확정 완료** |
| `FAIL_ALREADY_RESERVED` | 실패 | 이미 다른 사용자가 선점한 좌석 |
| `FAIL_OPTIMISTIC_CONFLICT` | 실패 | 동시 충돌로 인한 처리 실패 (재시도 권장) |

---

### 2.3. 실시간 알림 구독 (SSE)
- **URL**: `GET /api/reservations/v5/subscribe`
- **Query Parameters**: `userId`, `seatId`

**Events**
- `INIT`: 연결 시 데이터 `"Connected for Seat: {id}"`
- `RESERVATION_STATUS`: 최종 결과 데이터 `"SUCCESS"`, `"FAIL_ALREADY_RESERVED"` 등

---

## 🔒 3. 동기식 예약 (Sync - v1, v2, v3)
즉시 DB에 반영하고 결과를 리턴받는 방식입니다.

- **URL**: `POST /api/reservations/v3/distributed-lock` (분산 락 버전)

**Response (200 OK)**
```json
{
  "id": 7,
  "userId": 1,
  "seatId": 10,
  "reservationTime": "2026-02-05T21:04:19"
}
```

---

## 📖 4. 내 예약 관리 및 삭제

### 4.1. 유저별 예약 내역 조회
- **URL**: `GET /api/reservations/users/{userId}`

**Response (200 OK)**
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

### 4.2. 예약 취소
- **URL**: `DELETE /api/reservations/{id}`
- **Response**: `240 No Content` (성공 시 Body 없음)

---

## 🚨 5. 공통 에러 응답
```json
{
  "timestamp": "2026-02-05T21:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/reservations/v4/status"
}
```