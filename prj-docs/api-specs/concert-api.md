# 🎸 Concert API Specification

공연 정보 및 실시간 예약 가능한 좌석 현황을 제공하는 API입니다.

---

## 🎯 1. 데이터 모델 이해 (Conceptual Hierarchy)
본 서비스의 데이터는 다음 계층을 따릅니다.
- **Concert (공연)**: 제목, 아티스트 정보를 포함하는 최상위 객체.
- **ConcertOption (일정)**: 특정 공연의 상세 날짜/시간. (좌석의 부모)
- **Seat (좌석)**: 실제 예매 대상. 번호와 상태를 가집니다.

---

## 🛠️ 2. API 상세 명세

### 2.1. 공연 목록 조회
- **URL**: `GET /api/concerts`

**Response (200 OK)**
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

### 2.2. 공연 일정(날짜) 조회
- **URL**: `GET /api/concerts/{id}/options`

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "concertDate": "2026-02-15T19:00:00"
  }
]
```

---

### 2.3. 예약 가능 좌석 현황 조회
- **URL**: `GET /api/concerts/options/{optionId}/seats`

**Response (200 OK)**
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 좌석 고유 ID |
| `seatNumber` | String | 좌석 식별 번호 (예: A-1) |
| `status` | String | 현 상태 (AVAILABLE, RESERVED) |

```json
[
  {
    "id": 31,
    "seatNumber": "A-1",
    "status": "AVAILABLE"
  }
]
```

---

### 2.4. [Admin] 테스트 데이터 자동 셋업
- **URL**: `POST /api/concerts/setup`
- **Description**: 테스트를 위한 풀 세트 데이터를 즉시 생성합니다.

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
`"Setup completed: ConcertID=4, OptionID=7"`