package com.ticketrush.global.scheduler;

import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private final WaitingQueueService waitingQueueService;

    // 10초마다 대기열 상위 10명을 활성화
    @Scheduled(fixedDelay = 10000)
    public void activateWaitingUsers() {
        log.info(">>>> [Scheduler] 대기열 유저 활성화 시작...");
        // 테스트용으로 Concert ID 1번에 대해서만 처리
        waitingQueueService.activateUsers(1L, 10);
    }
}
