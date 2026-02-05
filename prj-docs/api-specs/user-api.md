# 👤 User API Specification

유저 정보의 생성, 조회, 삭제를 관리하는 API입니다.

---

## 1. 유저 생성 (회원가입)
- **Endpoint**: `POST /api/users`
- **Method**: `POST`

**Request Body**
```json
{
  "username": "tester1"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "username": "tester1"
}
```

---

## 2. 유저 단건 조회
- **Endpoint**: `GET /api/users/{id}`
- **Method**: `GET`

**Response (200 OK)**
```json
{
  "id": 1,
  "username": "tester1"
}
```

---

## 3. 유저 삭제
- **Endpoint**: `DELETE /api/users/{id}`
- **Method**: `DELETE`
- **Description**: 유저 정보를 삭제합니다. (연관된 데이터가 있을 경우 삭제가 제한될 수 있음)

**Response (204 No Content)**
- Body 없음.