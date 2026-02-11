package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.reservation")
public class ReservationProperties {

    /**
     * 결제 대기(HOLD/PAYING) 상태 유지 시간.
     */
    private long holdTtlSeconds = 300;

    /**
     * 만료 스캔 스케줄러 주기.
     */
    private long expireCheckDelayMillis = 5_000;
}
