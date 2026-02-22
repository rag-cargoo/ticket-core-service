package com.ticketrush.global.scheduler;

public interface SchedulerLockService {

    boolean runWithLock(String lockKey, Runnable task);
}
