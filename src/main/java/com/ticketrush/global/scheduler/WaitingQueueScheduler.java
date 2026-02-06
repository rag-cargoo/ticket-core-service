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
    private final com.ticketrush.global.config.WaitingQueueProperties properties;

    // 대기열 상위 유저를 주기적으로 활성화
    @Scheduled(fixedDelayString = "10000") // 10초
    public void activateWaitingUsers() {
        log.info(">>>> [Scheduler] 대기열 유저 활성화 시작 (Concert: {}, Count: {})", 
                properties.getActivationConcertId(), properties.getActivationBatchSize());
        
        waitingQueueService.activateUsers(properties.getActivationConcertId(), properties.getActivationBatchSize());
    }
}
