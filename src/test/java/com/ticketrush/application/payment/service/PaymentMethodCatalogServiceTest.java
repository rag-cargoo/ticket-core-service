package com.ticketrush.application.payment.service;

import com.ticketrush.application.payment.model.PaymentMethodCatalogResult;
import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.global.config.PaymentProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodCatalogServiceTest {

    @Test
    void walletProvider_shouldExposeWalletOnlyAsAvailable() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("wallet");

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);
        PaymentMethodCatalogResult catalog = service.getCatalog();

        assertThat(catalog.getProvider()).isEqualTo("wallet");
        assertThat(catalog.getDefaultMethod()).isEqualTo("WALLET");
        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("WALLET")
                        && item.isEnabled()
                        && item.getStatus().equals("AVAILABLE")
        );
        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("CARD")
                        && !item.isEnabled()
                        && item.getStatus().equals("PLANNED")
        );
    }

    @Test
    void overrides_shouldChangeStatusAndMessage() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("wallet");
        properties.setMethodStatusOverrides(Map.of("wallet", "maintenance"));
        properties.setMethodMessageOverrides(Map.of("wallet", "점검중"));

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);
        PaymentMethodCatalogResult catalog = service.getCatalog();

        assertThat(catalog.getMethods()).anyMatch(item ->
                item.getCode().equals("WALLET")
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
        properties.setProvider("wallet");

        PaymentMethodCatalogService service = new PaymentMethodCatalogService(properties);

        assertThatThrownBy(() -> service.assertMethodAvailable(PaymentMethod.CARD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment method unavailable");
    }
}
