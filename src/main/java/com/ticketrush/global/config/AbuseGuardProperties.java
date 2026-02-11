package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.abuse-guard")
public class AbuseGuardProperties {

    /**
     * 유저별 HOLD 요청 빈도 계산 윈도우.
     */
    private long holdRequestWindowSeconds = 10;

    /**
     * 윈도우 내 허용 HOLD 요청 수.
     */
    private long holdRequestMaxCount = 3;

    /**
     * requestFingerprint 중복 판단 윈도우.
     */
    private long duplicateRequestWindowSeconds = 300;

    /**
     * deviceFingerprint 다계정 탐지 윈도우.
     */
    private long deviceWindowSeconds = 600;

    /**
     * 동일 디바이스에서 허용할 최대 distinct user 수.
     */
    private long deviceMaxDistinctUsers = 1;

    /**
     * 운영 감사 조회 시 기본 limit.
     */
    private int auditQueryDefaultLimit = 100;
}
