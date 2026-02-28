package com.ticketrush.application.concert.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.concert-live")
public class ConcertLiveProperties {

    private Mode mode = Mode.WEBSOCKET;
    private long hybridPollIntervalMillis = 30_000L;

    public String normalizedMode() {
        return mode.name().toLowerCase(Locale.ROOT);
    }

    public enum Mode {
        WEBSOCKET,
        HYBRID,
        POLLING
    }
}
