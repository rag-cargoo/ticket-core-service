# Meeting Notes: Service Interface Split Rollout

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-16 06:22:19`
> - **Updated At**: `2026-02-16 06:26:15`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 안건 1: Service + ServiceImpl 표준화 범위 확정
> - 안건 2: 코드/테스트/스크립트/문서 동기화 규칙 확정
> - 안건 3: 이슈/브랜치 운영 절차 확정
<!-- DOC_TOC_END -->

## 안건 1: Service + ServiceImpl 표준화 범위 확정
- Created At: 2026-02-16 06:22:19
- Updated At: 2026-02-16 06:22:19
- Status: DOING
- 결정사항:
  - `Repository` 계층은 현행(`interface + Spring Data JPA 구현체 프록시`)을 유지한다.
  - 1차 리팩터링 대상은 `domain/auth/service`, `domain/reservation/service`로 한정한다.
  - API 경로/요청/응답 계약은 변경하지 않는다.
  - 이번 작업은 아키텍처 리팩터링 전용으로 `Payment Track P1`, `UX Track U1` 기능 추가/변경은 범위에서 제외한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: TODO
  - 메모: `Service + ServiceImpl` 네이밍으로 인터페이스/구현체를 분리하고 컨트롤러/글로벌 컴포넌트 주입 타입을 인터페이스로 전환.

## 안건 2: 코드/테스트/스크립트/문서 동기화 규칙 확정
- Created At: 2026-02-16 06:22:19
- Updated At: 2026-02-16 06:22:19
- Status: DOING
- 결정사항:
  - 리팩터링은 코드 단독 변경이 아니라 `코드 + 테스트 + 스크립트 + 문서`를 배치 단위로 함께 반영한다.
  - 클래스명/주입타입 변경 영향 파일은 전수 검색(`rg`)으로 누락 없이 동기화한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: TODO
  - 메모: 테스트(`src/test`), API 스크립트(`scripts/api`), HTTP 샘플(`scripts/http`), U1 페이지(`static/ux/u1`), 문서(`prj-docs/*`) 동기화 체크리스트 운영.

## 안건 3: 이슈/브랜치 운영 절차 확정
- Created At: 2026-02-16 06:22:19
- Updated At: 2026-02-16 06:26:15
- Status: DOING
- 결정사항:
  - 대규모 리팩터링은 전용 GitHub 이슈와 전용 브랜치에서 단계적으로 수행한다.
  - 이슈는 Reopen-first 정책을 따르고 진행 로그를 코멘트로 누적한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: TODO
  - 메모: 회의록/태스크 반영 후 `issue-upsert.sh`로 이슈 `#47`(https://github.com/rag-cargoo/2602/issues/47) 등록 완료, `chore/service-interface-split-prep` 브랜치에서 1차 배치 진행.
