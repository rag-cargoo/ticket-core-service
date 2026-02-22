package com.ticketrush.global.config;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    void configureMessageBroker_simpleMode_usesSimpleBroker() {
        WebSocketBrokerProperties brokerProperties = new WebSocketBrokerProperties();
        brokerProperties.setMode(WebSocketBrokerProperties.Mode.SIMPLE);

        WebSocketConfig config = new WebSocketConfig(List.of("http://localhost:8080"), brokerProperties);

        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        SimpleBrokerRegistration simpleBrokerRegistration = mock(SimpleBrokerRegistration.class);
        when(registry.enableSimpleBroker("/topic")).thenReturn(simpleBrokerRegistration);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry, never()).enableStompBrokerRelay("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void configureMessageBroker_relayMode_usesRelayBroker() {
        WebSocketBrokerProperties brokerProperties = new WebSocketBrokerProperties();
        brokerProperties.setMode(WebSocketBrokerProperties.Mode.RELAY);
        brokerProperties.setRelayHost("rabbitmq");
        brokerProperties.setRelayPort(61614);
        brokerProperties.setRelayClientLogin("client");
        brokerProperties.setRelayClientPasscode("client-pass");
        brokerProperties.setRelaySystemLogin("system");
        brokerProperties.setRelaySystemPasscode("system-pass");
        brokerProperties.setRelayVirtualHost("/");
        brokerProperties.setRelaySystemHeartbeatSendInterval(1111L);
        brokerProperties.setRelaySystemHeartbeatReceiveInterval(2222L);

        WebSocketConfig config = new WebSocketConfig(List.of("http://localhost:8080"), brokerProperties);

        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        StompBrokerRelayRegistration relayRegistration = mock(StompBrokerRelayRegistration.class, Answers.RETURNS_SELF);
        when(registry.enableStompBrokerRelay("/topic")).thenReturn(relayRegistration);

        config.configureMessageBroker(registry);

        verify(registry).enableStompBrokerRelay("/topic");
        verify(registry, never()).enableSimpleBroker("/topic");
        verify(relayRegistration).setRelayHost("rabbitmq");
        verify(relayRegistration).setRelayPort(61614);
        verify(relayRegistration).setClientLogin("client");
        verify(relayRegistration).setClientPasscode("client-pass");
        verify(relayRegistration).setSystemLogin("system");
        verify(relayRegistration).setSystemPasscode("system-pass");
        verify(relayRegistration).setVirtualHost("/");
        verify(relayRegistration).setSystemHeartbeatSendInterval(1111L);
        verify(relayRegistration).setSystemHeartbeatReceiveInterval(2222L);
        verify(registry).setApplicationDestinationPrefixes("/app");
    }
}
