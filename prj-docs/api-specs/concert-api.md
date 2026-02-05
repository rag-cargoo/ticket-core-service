# 🎸 Concert API Specification

공연, 공연 옵션(날짜), 그리고 좌석 정보를 조회하고 관리하는 API입니다.

---

## 1. 공연 목록 조회
- **Endpoint**: `GET /api/concerts`
- **Response (200 OK)**: `ConcertResponse[]`

**Object Detail**
- `id` (Long): 공연 고유 ID.
- `title` (String): 공연 제목.
- `artistName` (String): 출연 아티스트 이름.

---

## 2. 공연 일정(옵션) 조회
- **Endpoint**: `GET /api/concerts/{id}/options`
- **Response (200 OK)**: `ConcertOptionResponse[]`

**Object Detail**
- `id` (Long): 옵션 고유 ID.
- `concertDate` (DateTime): 공연 시작 일시.

---

## 3. 예약 가능 좌석 조회
- **Endpoint**: `GET /api/concerts/options/{optionId}/seats`
- **Response (200 OK)**: `SeatResponse[]`

**Object Detail**
- `id` (Long): 좌석 고유 ID.
- `seatNumber` (String): 좌석 번호 (예: "A-1").
- `status` (String): 상태 (`AVAILABLE`, `RESERVED`).

---

## 4. [Admin] 테스트 데이터 일괄 셋업
- **Endpoint**: `POST /api/concerts/setup`
- **Method**: `POST`

**Request Body**
```json
{
  "title": "NewJeans Special",
  "artistName": "NewJeans",
  "agencyName": "ADOR",
  "concertDate": "2026-03-01T18:00:00",
  "seatCount": 50
}
```

**Response (200 OK)**
- `"Setup completed: ConcertID=4, OptionID=7"`