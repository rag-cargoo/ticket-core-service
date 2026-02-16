# Meeting Notes: Concert Search Cache Batch2

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-17 02:32:49`
> - **Updated At**: `2026-02-17 04:01:06`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 안건 1: #52 배치2 범위 확정
> - 안건 2: 구현/검증 결과 정리
> - 안건 3: 잔여 작업/다음 액션
<!-- DOC_TOC_END -->

## 안건 1: #52 배치2 범위 확정
- Created At: 2026-02-17 02:32:49
- Updated At: 2026-02-17 02:32:49
- Status: DONE
- 결정사항:
  - 이슈 `#52`는 결제(P1)를 제외하고 Concert 탐색 경로를 우선 고도화한다.
  - 배치2 범위는 `캐시 무효화 정책 보강` + `U1 서버 검색 연동`으로 고정한다.
  - 캐시 정책은 `list/search/options/available-seats` 4개 read-cache를 기준으로 TTL/size를 설정형으로 운영한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: DONE
  - 메모: `application.yml`에 `APP_CACHE_CONCERT_*` 오버라이드 키를 추가하고 Caffeine 기반 CacheManager를 구성했다.

## 안건 2: 구현/검증 결과 정리
- Created At: 2026-02-17 02:32:49
- Updated At: 2026-02-17 02:32:49
- Status: DONE
- 결정사항:
  - 좌석 조회 캐시(`concert:available-seats`)를 추가하고, 예약 상태전이(`reserve/hold/confirm/cancel/expire`) 경로에서 option 단위 무효화를 연결한다.
  - U1 Concert Explorer는 `/api/concerts` 클라이언트 필터 방식을 제거하고 `/api/concerts/search` 서버 검색 + 페이지 버튼으로 전환한다.
  - 검색 입력은 250ms 디바운스로 호출량을 안정화한다.
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-17
  - 상태: DONE
  - 메모: `./gradlew compileJava`, `./gradlew test --tests '*ConcertExplorerIntegrationTest'`, `./gradlew test`, `validate-precommit-chain.sh --mode strict --all --strict-remote` 모두 PASS.

## 안건 3: 잔여 작업/다음 액션
- Created At: 2026-02-17 02:32:49
- Updated At: 2026-02-17 04:07:00
- Status: DONE
- 결정사항:
  - `#52`는 배치2까지 완료 기준을 충족했으므로 close 대상으로 정리한다.
  - 후속 우선순위는 `#53`(Artist/Agency 도메인 확장) -> `Payment Track P1` 순서로 진행한다.
  - U1의 결제 상태 패널은 P1 구현 시점에 결합한다.
  - 이슈/PR 링크:
    - Issue: https://github.com/rag-cargoo/2602/issues/52
    - PR: https://github.com/rag-cargoo/2602/pull/56
- 후속작업:
  - 담당: Codex
  - 기한: 2026-02-18
  - 상태: DONE
  - 메모: 후속 이슈 `#53`은 PR `#57`로 완료됐고, 결제(P1)는 현재 HOLD 정책으로 관리한다.
