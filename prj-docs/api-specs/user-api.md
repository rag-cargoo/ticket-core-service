# User API Specification

유저 생성 및 조회, 삭제를 담당하는 API입니다.

## 1. 유저 생성 (회원가입)
- **Endpoint**: `POST /api/users`
- **Description**: 새로운 유저를 등록합니다.

### Request Body
```json
{
  "username": "string"
}
```

### Response Body (200 OK)
```json
{
  "id": 1,
  "username": "tester1"
}
```

---

## 2. 유저 조회
- **Endpoint**: `GET /api/users/{id}`
- **Description**: 유저 정보를 조회합니다.

### Response Body (200 OK)
```json
{
  "id": 1,
  "username": "tester1"
}
```

---

## 3. 유저 삭제
- **Endpoint**: `DELETE /api/users/{id}`
- **Description**: 유저 정보를 삭제합니다.

### Response (240 No Content)
- No Body
