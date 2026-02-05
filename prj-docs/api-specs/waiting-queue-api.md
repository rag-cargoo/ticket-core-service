# 🎫 Waiting Queue API Specification

> **Base URL**: `/api/v1/waiting-queue`
> **Status**: Draft (Step 5 Implementation)

## 1. 개요 (Overview)
사용자가 선착순 예약에 참여하기 위해 대기열에 진입하고, 자신의 순번을 확인하는 API입니다.

---

## 2. API 리스트 (API List)

| 기능 | Method | Path | Auth |
| :--- | :--- | :--- | :--- |
| 대기열 진입 | POST | `/join` | User |
| 대기 상태 조회 | GET | `/status` | User |

---

## 3. 상세 명세 (Detailed Specs)

### 3.1. 대기열 진입 (Join Queue)
**[Definition]** 사용자를 특정 공연의 대기열에 등록합니다.

*   **Request**: `POST /api/v1/waiting-queue/join`
    ```json
    {
      "userId": 100,
      "concertId": 1
    }
    ```
*   **Response (202 Accepted)**:
    ```json
    {
      "userId": 100,
      "concertId": 1,
      "status": "WAITING",
      "rank": 450
    }
    ```

### 3.2. 대기 상태 조회 (Get Status)
**[Definition]** 자신의 현재 대기 순번과 상태를 조회합니다.

*   **Request**: `GET /api/v1/waiting-queue/status?userId=100&concertId=1`
*   **Response (200 OK)**:
    *   **Case 1: 대기 중**
        ```json
        {
          "status": "WAITING",
          "rank": 120
        }
        ```
    *   **Case 2: 진입 허용 (Active)**
        ```json
        {
          "status": "ACTIVE",
          "rank": 0,
          "token": "valid-access-token-123"
        }
        ```

---

## 4. 에러 코드 (Error Codes)
| Code | Message | Description |
| :--- | :--- | :--- |
| 429 | Too Many Requests | 대기열 진입 시도가 너무 빈번함 |
| 404 | Concert Not Found | 존재하지 않는 공연 ID |

---

## 5. 테스트 케이스 (Test Cases)
1. 사용자가 처음 진입 시 `WAITING` 상태와 0보다 큰 `rank`를 받는다.
2. 시간이 지나면 `rank`가 점진적으로 줄어든다.
3. 순번이 0이 되면 상태가 `ACTIVE`로 변한다.

---

## 6. 비고 (Notes)
- Redis Sorted Set을 사용하여 실시간 순번을 계산합니다.
- `ACTIVE` 상태로 전환된 유저는 5분 내에 예약을 완료해야 합니다.
