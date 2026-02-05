# 👤 User API Specification

티켓 서비스 이용을 위한 사용자 프로필을 생성하고 관리하는 API입니다.

---

## 🎯 1. 기능 개요
- 사용자는 고유한 `username`을 통해 시스템에 등록됩니다.
- 생성된 유저 ID는 예약 시 필수 식별자로 사용되므로 클라이언트는 이를 안전하게 보관해야 합니다.

---

## 🛠️ 2. API 상세 명세

### 2.1. 신규 유저 생성 (Sign-up)
- **URL**: `POST /api/users`
- **Method**: `POST`

**Request Body**
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `username` | String | Yes | 사용자 식별 이름 |

```json
{
  "username": "tester1"
}
```

**Response (200 OK)**
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | 시스템이 발급한 유저 고유 ID |
| `username` | String | 등록된 사용자 이름 |

```json
{
  "id": 1,
  "username": "tester1"
}
```

---

### 2.2. 유저 정보 조회
- **URL**: `GET /api/users/{id}`
- **Response (200 OK)**
```json
{
  "id": 1,
  "username": "tester1"
}
```

---

### 2.3. 유저 삭제
- **URL**: `DELETE /api/users/{id}`
- **Description**: 계정을 삭제합니다. 예약 내역이 있는 경우 정합성을 위해 삭제가 거부될 수 있습니다.
- **Response**: `204 No Content`