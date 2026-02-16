package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cache.concert")
public class ConcertCacheProperties {

    private Duration listTtl = Duration.ofSeconds(30);
    private long listMaxSize = 100;

    private Duration searchTtl = Duration.ofSeconds(20);
    private long searchMaxSize = 500;

    private Duration optionsTtl = Duration.ofSeconds(30);
    private long optionsMaxSize = 1_000;

    private Duration availableSeatsTtl = Duration.ofSeconds(5);
    private long availableSeatsMaxSize = 2_000;
}
