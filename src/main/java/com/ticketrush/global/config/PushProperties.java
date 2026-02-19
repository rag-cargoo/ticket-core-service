package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.push")
public class PushProperties {

    private Mode mode = Mode.WEBSOCKET;

    public enum Mode {
        SSE,
        WEBSOCKET
    }
}
