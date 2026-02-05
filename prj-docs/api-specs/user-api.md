# 👤 User API Specification

티켓 서비스 이용을 위한 사용자 프로필을 관리하는 API입니다. 모든 요청과 응답은 일관된 규격을 따릅니다.

---

## 🛠️ 1. API 상세 명세 (Endpoint Details)

### 1.1. 신규 유저 생성 (Sign-up)
- **Endpoint**: `POST /api/users`
- **Description**: 시스템 이용을 위한 새로운 유저를 등록합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Body | `username` | String | Yes | 사용자 식별 이름 (중복 불가) |

**Request Example**

```json
{
  "username": "tester1"
}
```

**Response Summary (200 OK)**

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 발급된 유저 고유 ID |
| `username` | String | 등록된 사용자 이름 |

**Response Example**

```json
{
  "id": 1,
  "username": "tester1"
}
```

---

### 1.2. 유저 단건 조회
- **Endpoint**: `GET /api/users/{id}`
- **Description**: ID를 기반으로 유저의 상세 정보를 조회합니다.

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `id` | Long | Yes | 조회할 유저 고유 ID |

**Response Example**

```json
{
  "id": 1,
  "username": "tester1"
}
```

---

### 1.3. 유저 삭제
- **Endpoint**: `DELETE /api/users/{id}`
- **Description**: 유저 계정을 삭제합니다. (진행 중인 예약이 있을 경우 실패할 수 있음)

**Parameters**

| Location | Field | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| Path | `id` | Long | Yes | 삭제할 유저 고유 ID |

**Response Summary (204 No Content)**

- 성공 시 응답 바디 없음.

---

## 🚨 2. 공통 에러 응답 (Common Error)

```json
{
  "timestamp": "2026-02-05T21:30:00.000",
  "status": 404,
  "error": "Not Found",
  "path": "/api/users/999"
}
```