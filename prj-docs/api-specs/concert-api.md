# Concert API Specification

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-17 02:50:20`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1. API 상세 명세 (Endpoint Details)
> - 2. 캐싱 정책/무효화 규칙
> - 3. UX Track U1 연동 계약 메모
<!-- DOC_TOC_END -->

공연 정보, 예약 가능 일정 및 좌석 현황을 제공하는 API입니다.

---

## 1. API 상세 명세 (Endpoint Details)

### 1.1. 전체 공연 목록 조회
- **Endpoint**: `GET /api/concerts`
- **Description**: 현재 시스템에 등록된 모든 공연 리스트를 조회합니다.

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 공연 고유 ID |
| `title` | String | 공연 제목 |
| `artistName` | String | 출연 아티스트 이름 |
| `artistId` | Long | 아티스트 ID |
| `artistDisplayName` | String | 아티스트 표시명(옵션) |
| `artistGenre` | String | 아티스트 장르(옵션) |
| `artistDebutDate` | Date | 아티스트 데뷔일(옵션) |
| `agencyName` | String | 기획사 이름(옵션) |
| `agencyCountryCode` | String | 기획사 국가코드(옵션) |
| `agencyHomepageUrl` | String | 기획사 홈페이지 URL(옵션) |

**Response Example**

```json
[
  {
    "id": 1,
    "title": "The Golden Hour",
    "artistName": "IU",
    "artistId": 10,
    "artistDisplayName": "IU",
    "artistGenre": "K-POP",
    "artistDebutDate": "2008-09-18",
    "agencyName": "EDAM",
    "agencyCountryCode": "KR",
    "agencyHomepageUrl": "https://www.edam-ent.com"
  }
]
```

---

### 1.1-1. 공연 검색/필터/정렬 + 페이징 조회
- **Endpoint**: `GET /api/concerts/search`
- **Description**: 키워드(제목/아티스트/기획사/공연ID) 검색, 아티스트/기획사 필터, 정렬, 페이징을 서버에서 수행합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Query | `keyword` | String | No | 제목/아티스트/공연 ID 검색 키워드 |
| Query | `artistName` | String | No | 아티스트명 정확 일치 필터(대소문자 무시) |
| Query | `agencyName` | String | No | 기획사명 정확 일치 필터(대소문자 무시) |
| Query | `page` | Integer | No | 페이지 번호(기본값 `0`) |
| Query | `size` | Integer | No | 페이지 크기(기본값 `20`) |
| Query | `sort` | String | No | 정렬 규칙(`id|title|artistName|agencyName`,`asc|desc`) 예: `title,asc` |

**Request Example**

`GET /api/concerts/search?keyword=iu&artistName=IU&agencyName=EDAM&page=0&size=10&sort=title,asc`

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `items` | Array | 공연 목록(`id`, `title`, `artistName` + Artist/Agency 확장 필드) |
| `page` | Integer | 현재 페이지 번호 |
| `size` | Integer | 페이지 크기 |
| `totalElements` | Long | 전체 검색 결과 건수 |
| `totalPages` | Integer | 전체 페이지 수 |
| `hasNext` | Boolean | 다음 페이지 존재 여부 |

**Response Example**

```json
{
  "items": [
    {
      "id": 1,
      "title": "The Golden Hour",
      "artistName": "IU",
      "artistId": 10,
      "artistDisplayName": "IU",
      "artistGenre": "K-POP",
      "artistDebutDate": "2008-09-18",
      "agencyName": "EDAM",
      "agencyCountryCode": "KR",
      "agencyHomepageUrl": "https://www.edam-ent.com"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
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
- **Description**: 선택한 공연 일정의 `AVAILABLE` 좌석만 조회합니다.

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
| Body | `artistDisplayName` | String | No | 아티스트 표시명 |
| Body | `artistGenre` | String | No | 아티스트 장르 |
| Body | `artistDebutDate` | Date | No | 아티스트 데뷔일 (`yyyy-mm-dd`) |
| Body | `agencyName` | String | Yes | 기획사 이름 |
| Body | `agencyCountryCode` | String | No | 기획사 국가코드 (`KR`, `JP`, ...) |
| Body | `agencyHomepageUrl` | String | No | 기획사 홈페이지 URL |
| Body | `concertDate` | DateTime | Yes | 공연 시작 일시 |
| Body | `seatCount` | Integer | Yes | 생성할 좌석 수 |

**Request Example**

```json
{
  "title": "NewJeans Special",
  "artistName": "NewJeans",
  "artistDisplayName": "NewJeans",
  "artistGenre": "K-POP",
  "artistDebutDate": "2022-07-22",
  "agencyName": "ADOR",
  "agencyCountryCode": "KR",
  "agencyHomepageUrl": "https://ador.world",
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

---

### 1.6. [Admin/Test] Step 11 판매 정책 생성/수정
- **Endpoint**: `PUT /api/concerts/{concertId}/sales-policy`
- **Description**: 공연별 판매 정책(선예매 기간, 선예매 최소 등급, 일반 오픈 시각, 1인 최대 예약 수)을 생성/수정합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `concertId` | Long | Yes | 대상 공연 ID |
| Body | `presaleStartAt` | DateTime | No | 선예매 시작 시각 |
| Body | `presaleEndAt` | DateTime | No | 선예매 종료 시각 (`generalSaleStartAt` 이하) |
| Body | `presaleMinimumTier` | String | No | 선예매 최소 등급 (`SILVER`/`GOLD`/`VIP`) |
| Body | `generalSaleStartAt` | DateTime | Yes | 일반 판매 시작 시각 |
| Body | `maxReservationsPerUser` | Integer | Yes | 1인 최대 동시 예약 수 (`>=1`) |

**Request Example**

```json
{
  "presaleStartAt": "2026-02-11T13:00:00",
  "presaleEndAt": "2026-02-11T13:30:00",
  "presaleMinimumTier": "VIP",
  "generalSaleStartAt": "2026-02-11T13:30:00",
  "maxReservationsPerUser": 1
}
```

**Response Example**

```json
{
  "id": 1,
  "concertId": 4,
  "presaleStartAt": "2026-02-11T13:00:00",
  "presaleEndAt": "2026-02-11T13:30:00",
  "presaleMinimumTier": "VIP",
  "generalSaleStartAt": "2026-02-11T13:30:00",
  "maxReservationsPerUser": 1
}
```

---

### 1.7. [Read] Step 11 판매 정책 조회
- **Endpoint**: `GET /api/concerts/{concertId}/sales-policy`
- **Description**: 대상 공연의 현재 판매 정책을 조회합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `concertId` | Long | Yes | 조회 대상 공연 ID |

**Response Summary (200 OK)**

- `PUT /api/concerts/{concertId}/sales-policy` 응답과 동일한 필드 구조를 반환합니다.

## 2. 캐싱 정책/무효화 규칙

Concert read-path 캐시는 `concert:list`, `concert:search`, `concert:options`, `concert:available-seats` 4종으로 운영한다.

| Cache Name | 대상 API | 기본 TTL | 무효화 트리거 |
| :--- | :--- | :--- | :--- |
| `concert:list` | `GET /api/concerts` | `30s` | 공연/옵션/좌석 생성, 공연 삭제 |
| `concert:search` | `GET /api/concerts/search` | `20s` | 공연/옵션/좌석 생성, 공연 삭제 |
| `concert:options` | `GET /api/concerts/{id}/options` | `30s` | 공연/옵션/좌석 생성, 공연 삭제 |
| `concert:available-seats` | `GET /api/concerts/options/{optionId}/seats` | `5s` | 좌석 생성/공연 삭제 + 예약 상태 전이(`reserve/hold/confirm/cancel/expire`) |

환경별 튜닝은 아래 env로 조정한다.

- `APP_CACHE_CONCERT_LIST_TTL`, `APP_CACHE_CONCERT_LIST_MAX_SIZE`
- `APP_CACHE_CONCERT_SEARCH_TTL`, `APP_CACHE_CONCERT_SEARCH_MAX_SIZE`
- `APP_CACHE_CONCERT_OPTIONS_TTL`, `APP_CACHE_CONCERT_OPTIONS_MAX_SIZE`
- `APP_CACHE_CONCERT_AVAILABLE_SEATS_TTL`, `APP_CACHE_CONCERT_AVAILABLE_SEATS_MAX_SIZE`

---

## 3. UX Track U1 연동 계약 메모

`src/main/resources/static/ux/u1/index.html` 기준으로 콘서트 탐색 섹션에서 아래 순서로 호출한다.

| UI 단계 | Endpoint | 클라이언트 처리 기준 |
| :--- | :--- | :--- |
| 콘서트 목록/검색/필터/정렬/페이징 | `GET /api/concerts/search` | `keyword`, `artistName`, `agencyName`, `sort`, `page`, `size`를 항상 서버에 전달해 탐색 처리(입력 디바운스 `250ms`) |
| 콘서트 선택 | `GET /api/concerts/{id}/options` | `concertDate` 오름차순으로 정렬 후 첫 옵션 자동 선택(최초 진입 시) |
| 옵션 선택 | `GET /api/concerts/options/{optionId}/seats` | `seatNumber` 오름차순 렌더링, `AVAILABLE`만 표시하는 필터 토글 지원 |
| 좌석 선택 | (추가 API 없음) | `AVAILABLE` 좌석만 선택 가능, 선택 즉시 Reservation v7 `seatId` 입력값 자동 채움 |

오류 응답은 상태 코드 그대로 콘솔 로그에 노출하고, 사용자 재시도는 동일 버튼 액션으로 처리한다.
