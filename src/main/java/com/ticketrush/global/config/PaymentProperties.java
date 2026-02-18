package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    /**
     * 예약 확정 시 차감되는 기본 티켓 금액.
     */
    private long defaultTicketPriceAmount = 100_000L;
}
