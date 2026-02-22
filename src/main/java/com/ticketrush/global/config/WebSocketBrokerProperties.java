package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.websocket.broker")
public class WebSocketBrokerProperties {

    private Mode mode = Mode.RELAY;
    private String relayHost = "localhost";
    private int relayPort = 61613;
    private String relayClientLogin = "guest";
    private String relayClientPasscode = "guest";
    private String relaySystemLogin = "guest";
    private String relaySystemPasscode = "guest";
    private String relayVirtualHost = "/";
    private long relaySystemHeartbeatSendInterval = 10_000L;
    private long relaySystemHeartbeatReceiveInterval = 10_000L;

    public enum Mode {
        SIMPLE,
        RELAY
    }
}
