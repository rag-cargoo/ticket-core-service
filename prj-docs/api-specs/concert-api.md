# 🎸 Concert API Specification

공연 정보, 예약 가능 일정 및 좌석 현황을 제공하는 API입니다.

---

## 🛠️ 1. API 상세 명세 (Endpoint Details)

### 1.1. 전체 공연 목록 조회
- **Endpoint**: `GET /api/concerts`
- **Description**: 현재 시스템에 등록된 모든 공연 리스트를 조회합니다.

**Response Summary (200 OK)**
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 공연 고유 ID |
| `title` | String | 공연 제목 |
| `artistName` | String | 출연 아티스트 이름 |

**Response Example**
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

### 1.2. 공연 날짜(옵션) 조회
- **Endpoint**: `GET /api/concerts/{id}/options`
- **Description**: 특정 공연의 예매 가능한 날짜와 시간 목록을 조회합니다.

**Parameters**
| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `id` | Long | Yes | 공연 고유 ID |

**Response Summary (200 OK)**
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 날짜 옵션 고유 ID |
| `concertDate` | DateTime | 공연 시작 일시 |

**Response Example**
```json
[
  {
    "id": 1,
    "concertDate": "2026-02-15T19:00:00"
  }
]
```

---

### 1.3. 실시간 좌석 현황 조회
- **Endpoint**: `GET /api/concerts/options/{optionId}/seats`
- **Description**: 선택한 공연 일정의 모든 좌석 상태를 실시간 조회합니다.

**Parameters**
| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `optionId` | Long | Yes | 날짜 옵션 고유 ID |

**Response Summary (200 OK)**
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 좌석 고유 ID |
| `seatNumber` | String | 좌석 식별 번호 |
| `status` | String | 현 상태 (`AVAILABLE` / `RESERVED`) |

**Response Example**
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

### 1.4. [Admin] 테스트 데이터 일괄 셋업
- **Endpoint**: `POST /api/concerts/setup`
- **Description**: 공연, 아티스트, 기획사, 좌석을 한 번에 생성하여 테스트 환경을 구축합니다.

**Parameters**
| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Body | `title` | String | Yes | 공연 제목 |
| Body | `artistName` | String | Yes | 아티스트 이름 |
| Body | `agencyName` | String | Yes | 기획사 이름 |
| Body | `concertDate` | DateTime | Yes | 공연 시작 일시 |
| Body | `seatCount` | Integer | Yes | 생성할 좌석 수 |

**Request Example**
```json
{
  "title": "NewJeans Special",
  "artistName": "NewJeans",
  "agencyName": "ADOR",
  "concertDate": "2026-03-01T18:00:00",
  "seatCount": 50
}
```

**Response Example**
`Setup completed: ConcertID=4, OptionID=7`

---

### 1.5. [Admin] 테스트 데이터 삭제 (Cleanup)
- **Endpoint**: `DELETE /api/concerts/cleanup/{concertId}`
- **Description**: 특정 공연과 연관된 모든 데이터(옵션, 좌석)를 영구 삭제합니다.

**Parameters**
| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `concertId` | Long | Yes | 삭제할 공연 ID |

**Response Summary (200 OK)**
`Cleanup completed for ConcertID: 4`
