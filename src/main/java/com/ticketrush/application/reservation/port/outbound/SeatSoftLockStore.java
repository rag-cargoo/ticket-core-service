package com.ticketrush.application.reservation.port.outbound;

import java.util.concurrent.TimeUnit;

public interface SeatSoftLockStore {

    boolean setIfAbsent(String key, String value, long ttl, TimeUnit unit);

    void set(String key, String value, long ttl, TimeUnit unit);

    String get(String key);

    void delete(String key);
}
