# Project Agent: Ticket Core Service (Rules)

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-09 01:38:36`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - Scope
> - Mandatory Load Order
> - Project Rules
> - Done Criteria
<!-- DOC_TOC_END -->

## Scope
이 문서는 `workspace/apps/backend/ticket-core-service` 프로젝트에만 적용된다.
다른 프로젝트에는 이 규칙을 전파하지 않는다.

## Mandatory Load Order
1. `workspace/apps/backend/ticket-core-service/README.md`
2. `workspace/apps/backend/ticket-core-service/prj-docs/task.md`
3. `workspace/apps/backend/ticket-core-service/prj-docs/rules/`

## Project Rules
1. 프로젝트 기준선 문서/디렉토리(`README.md`, `prj-docs/PROJECT_AGENT.md`, `prj-docs/task.md`, `prj-docs/rules/`)는 항상 존재해야 한다.
2. 문서 변경 시 기존 상세 내용을 삭제/요약하지 않고 구조화 중심으로 수정한다.
3. 프로젝트 코드/설정 변경 시 `task.md`, `api-specs/*.md`, `knowledge/*.md`를 함께 현행화한다.
4. API/대기열/SSE 동작 변경 시 `scripts/http/*.http`와 `scripts/api/*.sh`를 함께 현행화한다.
5. 위 3~4번 규칙은 커밋 시 `skills/bin/validate-precommit-chain.sh` 정책 엔진으로 자동 검증된다. 기본 모드는 `quick`, 마일스톤 커밋은 `strict`를 사용한다.
6. 문서 신규 생성/이동 시 `sidebar-manifest.md` 링크를 즉시 동기화한다.
7. 동시성/대기열/MSA 관련 결정은 `prj-docs/knowledge/`에 근거와 함께 기록한다.
8. `strict` 모드에서 `scripts/api/*.sh`가 stage된 커밋은 프로젝트 로컬 스크립트(`scripts/api/run-api-script-tests.sh`)로 실제 실행 검증을 수행한다.
9. `strict` 모드에서는 실행 검증 결과를 `prj-docs/api-test/latest.md`에 기록하고 함께 stage해야 커밋 가능하다.
10. 중요 커밋의 `strict` 전환은 사용자 승인 후 수행하고, 완료 후 기본 모드를 `quick`으로 복귀한다.
11. 에이전트 최종 보고에는 항상 현재 `pre-commit` 모드와 전환 명령(`precommit_mode.sh status|quick|strict`)을 포함한다.
12. `strict`에서 `knowledge/*.md`는 Failure-First/Before&After/Execution Log 품질 규칙을, `api-specs/*.md`는 6-Step 표준 토큰을 만족해야 커밋 가능하다.

## Done Criteria
1. 코드, 테스트, 문서(`task.md`, 필요 시 API 명세)가 서로 모순 없이 정합성을 가진다.
2. GitHub Pages에서 문서 링크/렌더링이 깨지지 않는다.
