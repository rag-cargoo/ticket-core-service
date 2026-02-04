# TODO List (Ticket Core Service)

## 🎯 Next Milestone: Step 3 - Redis 분산 락

- [ ] `build.gradle`에 Redisson 의존성 추가
- [ ] Redis 연결 설정 (RedisConfig 작성)
- [ ] `ReservationService`에 분산 락 기반 예약 로직 추가
- [ ] `동시성_테스트_3_분산_락.java` 작성 및 검증
- [ ] `동시성-제어-전략.md` 문서 업데이트 및 사이드바 확인

## 🚀 Future Milestones
- [ ] Kafka 기반 비동기 대기열 도입
- [ ] 부하 테스트(k6)를 통한 성능 비교 분석
- [ ] 프론트엔드 연동 및 통합 테스트
