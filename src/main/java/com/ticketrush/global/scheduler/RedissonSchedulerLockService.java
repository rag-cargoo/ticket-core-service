package com.ticketrush.global.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedissonSchedulerLockService implements SchedulerLockService {

    private final RedissonClient redissonClient;
    private final long waitSeconds;
    private final long leaseSeconds;

    public RedissonSchedulerLockService(
            RedissonClient redissonClient,
            @Value("${app.scheduler.lock.wait-seconds:0}") long waitSeconds,
            @Value("${app.scheduler.lock.lease-seconds:30}") long leaseSeconds
    ) {
        this.redissonClient = redissonClient;
        this.waitSeconds = waitSeconds;
        this.leaseSeconds = leaseSeconds;
    }

    @Override
    public boolean runWithLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                log.debug("scheduler lock not acquired: {}", lockKey);
                return false;
            }
            task.run();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("scheduler lock interrupted: {}", lockKey, e);
            return false;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
