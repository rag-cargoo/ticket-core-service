package com.ticketrush.global.config;

import com.ticketrush.application.reservation.port.outbound.PaymentConfigPort;
import com.ticketrush.application.payment.port.outbound.PaymentMethodConfigPort;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties implements PaymentConfigPort, PaymentMethodConfigPort {

    /**
     * 예약 결제 처리 게이트웨이 provider.
     * 지원값: mock, pg-ready
     */
    private String provider = "mock";

    /**
     * 예약 확정 시 차감되는 기본 티켓 금액.
     */
    private long defaultTicketPriceAmount = 100_000L;

    /**
     * 외부 실결제 연동 활성화 여부.
     * false면 pg-ready는 승인콜백 준비 단계로 동작한다.
     */
    private boolean externalLiveEnabled = false;

    /**
     * pg-ready 외부 결제창 base URL.
     * 예: https://pg-ready.example.com/checkout
     */
    private String pgReadyCheckoutBaseUrl = "https://pg-ready.example.com/checkout";

    /**
     * pg-ready 승인 webhook URL.
     * 상대경로 또는 절대 URL 허용.
     */
    private String pgReadyCallbackUrl = "/api/payments/webhooks/pg-ready";

    /**
     * pg-ready 결제 성공 후 프론트 복귀 URL.
     */
    private String pgReadySuccessRedirectUrl = "/service?payment=success";

    /**
     * pg-ready 결제 취소 후 프론트 복귀 URL.
     */
    private String pgReadyCancelRedirectUrl = "/service?payment=cancel";

    /**
     * pg-ready 결제 실패 후 프론트 복귀 URL.
     */
    private String pgReadyFailureRedirectUrl = "/service?payment=failed";

    /**
     * 결제수단 상태 override.
     * 예: app.payment.method-status-overrides.card=maintenance
     */
    private Map<String, String> methodStatusOverrides = new LinkedHashMap<>();

    /**
     * 결제수단 안내문구 override.
     * 예: app.payment.method-message-overrides.card=점검중
     */
    private Map<String, String> methodMessageOverrides = new LinkedHashMap<>();
}
