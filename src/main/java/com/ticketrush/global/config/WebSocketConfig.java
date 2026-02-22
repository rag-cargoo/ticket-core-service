package com.ticketrush.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> normalizedAllowedOrigins;
    private final WebSocketBrokerProperties brokerProperties;

    public WebSocketConfig(
            @Value("${app.frontend.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}") List<String> allowedOrigins,
            WebSocketBrokerProperties brokerProperties
    ) {
        this.normalizedAllowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        this.brokerProperties = brokerProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(normalizedAllowedOrigins.toArray(new String[0]));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (brokerProperties.getMode() == WebSocketBrokerProperties.Mode.RELAY) {
            log.info(
                    "WebSocket broker mode: RELAY host={} port={}",
                    brokerProperties.getRelayHost(),
                    brokerProperties.getRelayPort()
            );
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(brokerProperties.getRelayHost())
                    .setRelayPort(brokerProperties.getRelayPort())
                    .setClientLogin(brokerProperties.getRelayClientLogin())
                    .setClientPasscode(brokerProperties.getRelayClientPasscode())
                    .setSystemLogin(brokerProperties.getRelaySystemLogin())
                    .setSystemPasscode(brokerProperties.getRelaySystemPasscode())
                    .setVirtualHost(brokerProperties.getRelayVirtualHost())
                    .setSystemHeartbeatSendInterval(brokerProperties.getRelaySystemHeartbeatSendInterval())
                    .setSystemHeartbeatReceiveInterval(brokerProperties.getRelaySystemHeartbeatReceiveInterval());
        } else {
            log.info("WebSocket broker mode: SIMPLE");
            registry.enableSimpleBroker("/topic");
        }
        registry.setApplicationDestinationPrefixes("/app");
    }
}
