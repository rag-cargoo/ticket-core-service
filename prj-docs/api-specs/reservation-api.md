# 🎫 Reservation API Specification & Integration Guide

이 문서는 프론트엔드 및 클라이언트 작업자를 위한 **티켓 예매 API 연동 가이드**입니다. 
동기식 처리와 비동기(대기열) 처리의 흐름을 명확히 구분하여 기술합니다.

---

## 🔄 1. 전체 예약 프로세스 (Integration Workflow)

### [Scenario A] 비동기 대기열 방식 (v4, v5) - 추천 ⭐
대규모 트래픽 발생 시 사용자가 대기열에 진입하고 실시간으로 결과를 받는 방식입니다.

1. **Step 1 (요청)**: `POST /api/reservations/v4-opt/queue-polling` 호출
2. **Step 2 (대기)**: 서버로부터 `202 Accepted` 응답 수신
3. **Step 3 (확인)**: 다음 두 가지 방법 중 선택하여 결과 확인
   - **방법 A (Polling)**: `GET /api/reservations/v4/status`를 1~2초 간격으로 반복 호출
   - **방법 B (SSE)**: `GET /api/reservations/v5/subscribe`를 통해 실시간 알림 구독
4. **Step 4 (완료)**: 상태가 `SUCCESS`로 변경되면 예약 완료 화면 노출

---

## 🛠️ 2. API 상세 명세 (Endpoint Details)

### 2.1. 비동기 예약 요청 (Entry)
- **URL**: `POST /api/reservations/v4-opt/queue-polling` (낙관적 락 전략)
- **URL**: `POST /api/reservations/v4-pes/queue-polling` (비관적 락 전략)
- **URL**: `POST /api/reservations/v5-opt/queue-sse` (SSE 알림용)
- **Headers**: `Content-Type: application/json`

**Request Body**
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
- **URL**: `GET /api/reservations/v4/status?userId={userId}&seatId={seatId}`
- **Method**: `GET`

**Response (200 OK)**
```json
{
  "status": "PENDING" 
}
```

| status 값 | 의미 | 프론트엔드 처리 가이드 |
| :--- | :--- | :--- |
| `PENDING` | 대기 중 | "대기열에서 차례를 기다리고 있습니다" 메시지 노출 |
| `PROCESSING` | 처리 중 | "예약을 확정하는 중입니다..." (로딩 바) |
| `SUCCESS` | 성공 | 예약 완료 페이지로 이동 |
| `FAIL` | 실패 | "좌석 선점에 실패했습니다." 안내 및 뒤로가기 |
| `NOT_FOUND` | 정보 없음 | 잘못된 요청이거나 만료된 요청 |

---

### 2.3. [SSE] 실시간 알림 구독
- **URL**: `GET /api/reservations/v5/subscribe?userId={userId}&seatId={seatId}`
- **Headers**: `Accept: text/event-stream`

**Event Types**
1. **`INIT`**: 연결 성공 시 즉시 발생.
   - Data: `"Connected for Seat: {id}"`
2. **`RESERVATION_STATUS`**: 비동기 처리가 끝나는 순간 단 한 번 발생.
   - Data: `"SUCCESS"` or `"FAIL"`

**연동 팁 (JavaScript)**
```javascript
const eventSource = new EventSource('/api/reservations/v5/subscribe?userId=1&seatId=10');

eventSource.addEventListener('RESERVATION_STATUS', (event) => {
    if (event.data === 'SUCCESS') {
        alert('예약 성공!');
    }
    eventSource.close(); // 결과 수신 후 반드시 연결 종료
});
```

---

## 🔒 3. 동기식 예약 (Legacy/Direct)
즉시 결과를 반환받는 방식입니다. (대규모 트래픽 시 타임아웃 발생 위험 높음)

- **URL**: `POST /api/reservations/v3/distributed-lock` (분산 락)
- **Response (200 OK)**
```json
{
  "id": 1,
  "userId": 1,
  "seatId": 10,
  "reservationTime": "2026-02-05T14:30:00"
}
```