package com.ticketrush.application.payment.service;

import com.ticketrush.application.payment.model.PaymentMethodCatalogResult;
import com.ticketrush.global.config.PaymentProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodCatalogServiceTest {

    @Test
    void mockProvider_shouldExposeCardOnlyAsAvailable() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("mock");

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);
        PaymentMethodCatalogResult catalog = service.getCatalog();

        assertThat(catalog.getProvider()).isEqualTo("mock");
        assertThat(catalog.getDefaultMethod()).isEqualTo("CARD");
        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("CARD")
                        && item.isEnabled()
                        && item.getStatus().equals("AVAILABLE")
                        && item.getMessage().contains("가상 테스트 카드")
        );
        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("KAKAOPAY")
                        && !item.isEnabled()
                        && item.getStatus().equals("PLANNED")
        );
    }

    @Test
    void overrides_shouldChangeStatusAndMessage() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("mock");
        properties.setMethodStatusOverrides(Map.of("card", "maintenance"));
        properties.setMethodMessageOverrides(Map.of("card", "점검중"));

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);
        PaymentMethodCatalogResult catalog = service.getCatalog();

        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("CARD")
                        && !item.isEnabled()
                        && item.getStatus().equals("MAINTENANCE")
                        && item.getMessage().equals("점검중")
        );
    }

    @Test
    void pgReadyProvider_shouldExposeIntegrationModeAndLiveFlag() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("pg-ready");
        properties.setExternalLiveEnabled(false);

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);
        PaymentMethodCatalogResult catalog = service.getCatalog();

        assertThat(catalog.getProviderMode()).isEqualTo("PG_WEBHOOK_READY");
        assertThat(catalog.isExternalLiveEnabled()).isFalse();
        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("CARD")
                        && item.isEnabled()
                        && item.getStatus().equals("AVAILABLE")
                        && item.getMessage().contains("실결제 미연동")
        );
    }

    @Test
    void assertMethodAvailable_shouldThrowWhenMethodIsUnavailable() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("mock");

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);

        assertThatThrownBy(() -> service.assertMethodAvailable("KAKAOPAY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment method unavailable");
    }
}
