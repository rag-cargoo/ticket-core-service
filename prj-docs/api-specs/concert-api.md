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

---

## 4. [Admin/Test] 공연 및 좌석 일괄 셋업
- **Endpoint**: `POST /api/concerts/setup`
- **Description**: 테스트를 위해 공연, 아티스트, 기획사, 옵션, 좌석을 한 번에 생성합니다.

### Request Body
```json
{
  "title": "NewJeans Bunnies Camp",
  "artistName": "NewJeans",
  "agencyName": "ADOR",
  "concertDate": "2026-03-01T18:00:00",
  "seatCount": 50
}
```

### Response (200 OK)
- Body: `Setup completed: ConcertID=4, OptionID=7`
