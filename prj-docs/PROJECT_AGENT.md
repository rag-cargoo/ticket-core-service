# Project Agent Scope: Ticket Core Service

## Scope
이 문서는 `workspace/apps/backend/ticket-core-service` 프로젝트에만 적용된다.
다른 프로젝트에는 이 규칙을 전파하지 않는다.

## Mandatory Load Order
1. `workspace/apps/backend/ticket-core-service/prj-docs/task.md`
2. `workspace/apps/backend/ticket-core-service/prj-docs/TODO.md`
3. `workspace/apps/backend/ticket-core-service/prj-docs/ROADMAP.md`
4. `workspace/apps/backend/ticket-core-service/prj-docs/rules/architecture.md`

## Project Rules
1. 문서 변경 시 기존 상세 내용을 삭제/요약하지 않고 구조화 중심으로 수정한다.
2. 프로젝트 코드/설정 변경 시 `task.md`, `TODO.md`, `api-specs/*.md`, `knowledge/*.md`를 함께 현행화한다.
3. API/대기열/SSE 동작 변경 시 `scripts/http/*.http`와 `scripts/api/*.sh`를 함께 현행화한다.
4. 위 2~3번 규칙은 커밋 시 `skills/bin/validate-ticket-core-chain.sh`로 자동 검증된다.
5. 문서 신규 생성/이동 시 `sidebar-manifest.md` 링크를 즉시 동기화한다.
6. 동시성/대기열/MSA 관련 결정은 `prj-docs/knowledge/`에 근거와 함께 기록한다.
7. `scripts/api/*.sh`가 stage된 커밋은 `skills/bin/run-ticket-core-api-script-tests.sh`를 통해 실제 실행 검증을 수행한다.
8. 실행 검증 결과는 `prj-docs/api-test/latest.md`에 기록하고 함께 stage해야 커밋 가능하다.

## Done Criteria
1. 코드, 테스트, 문서(`task.md`, 필요 시 API 명세)가 서로 모순 없이 정합성을 가진다.
2. GitHub Pages에서 문서 링크/렌더링이 깨지지 않는다.
