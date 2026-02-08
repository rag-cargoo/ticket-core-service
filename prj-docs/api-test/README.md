# API Script Test Guide

<!-- DOC_META_START -->
> [!NOTE]
> - **Created At**: `2026-02-08 23:07:03`
> - **Updated At**: `2026-02-08 23:11:27`
<!-- DOC_META_END -->

<!-- DOC_TOC_START -->
## 문서 목차 (Quick Index)
---
> [!TIP]
> - 1. 실행 방법
> - 2. 산출물
> - 3. 트러블슈팅 기준
<!-- DOC_TOC_END -->

`scripts/api/*.sh` 실행 검증과 결과 기록 규칙입니다.

---

## 1. 실행 방법

```bash
cd workspace/apps/backend/ticket-core-service
make test-suite
```

- 내부적으로 `scripts/api/run-api-script-tests.sh`를 호출합니다.
- 기본 실행 세트는 `v1`~`v7` 스크립트입니다.
- 기본 헬스체크 URL은 `http://127.0.0.1:8080/api/concerts` 입니다.
- 필요하면 `API_SCRIPT_HEALTH_URL` 환경변수로 변경할 수 있습니다.
- 기존 환경과의 호환을 위해 `TICKETRUSH_HEALTH_URL`도 별칭으로 지원합니다.

---

## 2. 산출물

- 최신 실행 리포트: `prj-docs/api-test/latest.md`
- 커밋 체인에서 `scripts/api/*.sh` 변경이 감지되면 위 리포트를 함께 stage해야 합니다.

---

## 3. 트러블슈팅 기준

1. 백엔드 헬스체크 실패 시 앱/인프라를 먼저 기동한다.
2. 스크립트 실패 시 `latest.md`의 실패 로그 요약을 확인한다.
3. 수정 후 테스트 재실행하여 리포트를 최신화한다.
4. 로컬 Kafka 접속 실패 시 `./gradlew bootRun --args='--spring.profiles.active=local --spring.kafka.bootstrap-servers=localhost:9092'`로 재기동한다.
