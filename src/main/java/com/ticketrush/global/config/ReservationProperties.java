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
     * 좌석 선택(soft lock) 유지 시간.
     */
    private long softLockTtlSeconds = 30;

    /**
     * 좌석 soft lock Redis key prefix.
     */
    private String softLockKeyPrefix = "seat:lock:";

    /**
     * 만료 스캔 스케줄러 주기.
     */
    private long expireCheckDelayMillis = 5_000;

    /**
     * 공연 시작 기준 환불 허용 마감 시간(시간 단위).
     * 기본값 24는 공연 시작 24시간 전까지만 환불을 허용한다.
     */
    private long refundCutoffHoursBeforeConcert = 24;
}
