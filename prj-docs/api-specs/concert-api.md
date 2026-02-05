# Concert API Specification

공연 목록 및 날짜, 좌석 정보를 조회하는 API입니다.

## 1. 공연 목록 조회
- **Endpoint**: `GET /api/concerts`
- **Description**: 현재 등록된 모든 공연 목록을 조회합니다.

### Response Body (200 OK)
```json
[
  {
    "id": 1,
    "title": "The Golden Hour",
    "artistName": "IU"
  }
]
```

---

## 2. 공연 일정(옵션) 조회
- **Endpoint**: `GET /api/concerts/{id}/options`
- **Description**: 특정 공연의 날짜별 예매 옵션을 조회합니다.

### Response Body (200 OK)
```json
[
  {
    "id": 1,
    "concertDate": "2026-02-15T15:31:40"
  }
]
```

---

## 3. 예약 가능 좌석 조회
- **Endpoint**: `GET /api/concerts/options/{optionId}/seats`
- **Description**: 특정 공연 일정의 예약 가능한 좌석 현황을 조회합니다.

### Response Body (200 OK)
```json
[
  {
    "id": 1,
    "seatNumber": "A-1",
    "status": "AVAILABLE"
  }
]
```
