# Project Task Dashboard - Ticket Core Service

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-11 07:15:00`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 단계 목차 (Step Index)
---
> [!TIP]
> - Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
> - Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
> - Step 3: Redis 분산 락(Redisson) 구현 및 검증
> - Step 4: Kafka 기반 비동기 대기열(Waiting Queue) 구현 및 검증
> - Step 5: Redis Sorted Set 기반 실시간 대기 순번 시스템 구현
> - Step 6: 대기열 진입 제한(Throttling) 및 유입량 제어 전략 구현
> - Step 7: SSE 기반 실시간 순번 자동 푸시 시스템 구현 및 회귀 검증
> - Step 8: k6 성능 기준선 확정 및 병목 제거
> - Step 9: 결제/좌석 점유 상태머신(홀드/확정/만료) 구현
> - Step 10: 취소/환불/재판매 대기열 연계 구현
> - Step 11: 판매 정책 엔진(선예매/등급/1인 제한) 구현
> - Step 12: 부정사용 방지/감사 추적 기능 구현
> - Step 0 (락 없음): Race Condition 발생 확인 (30명 중 10명 중복 예약)
<!-- DOC_TOC_END -->

> 시스템의 현재 상태와 단계별 목표, 세부 완료 내역을 추적하는 통합 보드입니다.

---

## 현재 상태 (Status)
---
> [!NOTE]
>   - **현재 단계**: Step 7 완료, Step 8 착수 준비
>   - **목표**: 고성능 선착순 티켓팅 시스템 구현
>   - **Tech Stack**: Java 17 / Spring Boot 3.4.1 / JPA / Redisson / PostgreSQL / Redis / Kafka / SSE
>   - **검증 체인**: pre-commit `quick`(기본) / `strict`(중요 커밋) 모드 운영, strict에서 문서/HTTP/API스크립트 동기화 + 실행 리포트 강제 검증

---

## 개발 원칙 (Dev Principles)
---
> [!TIP]
>   - **기술 비교/검증**: API 버전을 분리하여 관리 (v1~v4).
>   - **성능 측정**: 각 단계별 부하 테스트 결과를 기록하여 의사결정 근거로 활용.
>   - **문서화 필수**: 실험 결과와 의사결정 과정은 prj-docs/knowledge/에 상세히 기록.
>   - **안전 우선**: 파일 수정 전 원본 확인 및 파괴적 변경 시 사용자 보고 의무화.

---

## 당면 과제 (Current Tasks)
---
> [!NOTE]
>   - [x] Step 1: 낙관적 락(Optimistic Lock) 구현 및 검증
>   - [x] Step 2: 비관적 락(Pessimistic Lock) 구현 및 검증
>   - [x] Step 3: Redis 분산 락(Redisson) 구현 및 검증
>   - [x] Step 4: Kafka 기반 비동기 대기열(Waiting Queue) 구현 및 검증
>   - [x] Step 5: Redis Sorted Set 기반 실시간 대기 순번 시스템 구현
>   - [x] Step 6: 대기열 진입 제한(Throttling) 및 유입량 제어 전략 구현
>   - [x] Step 7: SSE 기반 실시간 순번 자동 푸시 시스템 구현 및 회귀 검증

---

## 운영 안정화 체크리스트 (Merged TODO)
---
> [!NOTE]
> 이 섹션은 기존 `prj-docs/TODO.md` 내용을 `task.md`로 통합한 항목입니다.
>
> - [x] 대기열 SSE 구독 엔드포인트 추가 (`/api/v1/waiting-queue/subscribe`)
> - [x] 대기 순번 변경 이벤트(`RANK_UPDATE`) 자동 푸시 흐름 완성
> - [x] 활성 전환 시점(`ACTIVE`) 알림 페이로드 표준화
> - [x] 타임아웃/재연결 포함 SSE 연결 수명주기 안정화
> - [x] API 변경 시 문서/HTTP/API스크립트 자동 체인 검증(pre-commit quick/strict) 구성
> - [x] API 스크립트 실행 결과 리포트 자동 생성 (`prj-docs/api-test/latest.md`)
> - [x] Step 7 API 명세/HTTP 파일/스크립트 최종 동기화 및 회귀 검증
> - [x] Step 7 운영 회귀 테스트 실행 스크립트 정비 (`scripts/api/run-step7-regression.sh`)

---

## 후속 백로그 (Merged TODO)
---
> [!WARNING]
> Step 7 이후 잔여 작업과 차기 단계 선행 준비 항목입니다.
>
> - [x] develop -> main 릴리즈 PR 및 Pages 최종 검증 수행 (Issue: `#28`, PR: `#46`)
> - [ ] 부하 테스트(k6)를 통한 임계치 측정 및 보고서 작성 (`make test-k6`, 리포트: `prj-docs/api-test/k6-latest.md`)
> - [ ] 프론트엔드 연동 및 통합 시나리오 검증
> - [ ] 공연 조회 캐싱 전략 도입
> - [ ] 아티스트/기획사 엔티티 확장

---

## 다음 단계 로드맵 (Step 8+)
---
> [!TIP]
> 기능 고도화는 스텝 단위로 진행하며, 각 스텝은 `목표 / 완료 기준 / 다음 액션`을 명시합니다.
>
> - [ ] **Step 8: k6 성능 기준선 확정 및 병목 제거**
>   - 목표: `join/status/subscribe` 기준 처리량, p95, 에러율 기준선을 확정한다.
>   - 완료 기준: `prj-docs/api-test/k6-latest.md`에 before/after와 병목 원인/개선 근거를 기록한다.
>   - 다음 액션: `make test-k6` 기본 시나리오 실행 후 병목 구간 1차 식별.
>
> - [ ] **Step 9: 결제/좌석 점유 상태머신(홀드/확정/만료) 구현**
>   - 목표: 예약 이후 결제 단계까지 상태 전이를 일관된 도메인 규칙으로 통합한다.
>   - 완료 기준: 상태 전이 다이어그램/API 명세/회귀 스크립트가 함께 갱신된다.
>   - 다음 액션: `HOLD -> PAYING -> CONFIRMED|EXPIRED` 전이 규칙과 TTL 정책 정의.
>
> - [ ] **Step 10: 취소/환불/재판매 대기열 연계 구현**
>   - 목표: 취소 좌석을 대기열과 안전하게 재연결하는 재판매 플로우를 완성한다.
>   - 완료 기준: 취소/환불 API + 재판매 이벤트 처리 + 데이터 정합성 테스트를 통과한다.
>   - 다음 액션: 취소 이벤트 스키마와 재할당 우선순위 규칙 정의.
>
> - [ ] **Step 11: 판매 정책 엔진(선예매/등급/1인 제한) 구현**
>   - 목표: 고정 로직이 아닌 정책 기반으로 판매 조건을 제어한다.
>   - 완료 기준: 정책 변경이 코드 수정 없이 설정/테이블 중심으로 반영된다.
>   - 다음 액션: 정책 모델(기간/등급/수량 제한)과 검증 인터셉터 구조 설계.
>
> - [ ] **Step 12: 부정사용 방지/감사 추적 기능 구현**
>   - 목표: 우회/봇/중복 시도를 탐지하고 추적 가능한 감사 이력을 남긴다.
>   - 완료 기준: 차단 규칙, 감사 로그, 운영 조회 API까지 연결된다.
>   - 다음 액션: rate rule + device/account fingerprint 최소 규칙 초안 작성.

---

## 진행된 세부 작업 (Completed Details)
---
> ### 1. 동시성 제어 실험 (Concurrency Challenge)
> ---
>   - **Step 0 (락 없음)**: Race Condition 발생 확인 (30명 중 10명 중복 예약).
>   - **Step 1 (낙관적 락)**: JPA @Version을 통한 충돌 감지 및 정합성 보장 확인.
>   - **Step 2 (비관적 락)**: SELECT ... FOR UPDATE를 통한 순차 처리 및 정합성 보장 확인.
>
> ### 2. API Layer Implementation
> ---
>   - **Concert API**: 목록 조회, 옵션 조회, 예약 가능 좌석 조회 엔드포인트 구현.
>   - **Reservation API**: 예약 요청 처리, 비관적 락 로직 및 일반 예약 구현.
>   - **DTOs**: ConcertResponse, SeatResponse, ReservationRequest 등 규격화.
>
> ### 3. Core Domain & DB Layer
> ---
>   - **Domain**: Concert, Seat, Reservation, User 엔티티 및 Repository 구현.
>   - **Infrastructure**: JPA 설정 및 H2/PostgreSQL 연동 확인.
>   - **Data Init**: K-POP 콘서트 더미 데이터 초기화 로직 구현.
>
> ### 4. 프로젝트 아키텍처 정교화 및 인프라 안정화
> ---
>   - **레이어 분리**: api, domain, global 계층 구조 확립.
>   - **DTO 정규화**: 모든 Java record를 class + Lombok 스타일로 전환하여 유연성 확보.
>   - **인프라 안정화**: docker-compose.yml 헬스체크 및 의존성 최적화.
