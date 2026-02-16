# Meeting Notes: Service Boundary Port Phase2 Kickoff

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-16 15:00:58`
> - **Updated At**: `2026-02-16 15:00:58`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 안건 1: Phase2 범위 및 비범위 확정
> - 안건 2: 문서/이슈/브랜치 운영 절차 확정
> - 안건 3: 배치 적용 순서 및 완료 기준 확정
<!-- DOC_TOC_END -->

## 안건 1: Phase2 범위 및 비범위 확정
- Created At: 2026-02-16 15:00:58
- Updated At: 2026-02-16 15:00:58
- Status: DOING
- 결정사항:
  - `Service + ServiceImpl` 1차(Auth/Reservation) 완료 이후, Phase2는 Reservation 도메인의 경계 의존을 Port/Adapter로 명시화한다.
  - 우선 대상은 변경 가능성이 큰 경계(`User`, `Seat`, `WaitingQueue`)이며, API 계약(URI/Request/Response)은 유지한다.
  - 이번 배치에서도 `Payment Track P1`, `UX Track U1` 기능 추가/변경은 범위에서 제외한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: TODO
  - 메모: `Reservation` 내부에서 직접적인 외부 도메인 접근 의존을 Port 인터페이스로 치환하는 후보를 식별한다.

## 안건 2: 문서/이슈/브랜치 운영 절차 확정
- Created At: 2026-02-16 15:00:58
- Updated At: 2026-02-16 15:04:15
- Status: DOING
- 결정사항:
  - 작업 시작 전에 회의록/태스크/이슈/전용 브랜치를 먼저 생성하고 진행한다.
  - 이슈 라이프사이클은 Reopen-first 정책을 우선 검토하되, 범위가 변경된 Phase2는 신규 이슈 생성 가능 대상으로 판단한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: TODO
  - 메모: 이 문서와 `task.md`, GitHub 이슈 `#49`(https://github.com/rag-cargoo/2602/issues/49), 브랜치 `chore/service-interface-boundary-phase2`를 상호 링크로 동기화한다.

## 안건 3: 배치 적용 순서 및 완료 기준 확정
- Created At: 2026-02-16 15:00:58
- Updated At: 2026-02-16 15:00:58
- Status: DOING
- 결정사항:
  - 배치 1: Port 인터페이스 추가 + 기존 구현 래핑(동작 불변)
  - 배치 2: Reservation 서비스 주입 타입 전환 + 단위/통합 테스트 동기화
  - 배치 3: 문서/스크립트/운영가이드 최신화 및 전체 회귀(`./gradlew test`)
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: TODO
  - 메모: 각 배치 완료 시점마다 근거 커밋과 검증 로그를 이슈 코멘트에 누적한다.
