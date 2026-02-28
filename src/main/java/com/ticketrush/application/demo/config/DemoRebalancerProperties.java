package com.ticketrush.application.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.demo-rebalancer")
public class DemoRebalancerProperties {
    private boolean enabled = false;
    private int defaultIntervalMinutes = 60;
    private List<Integer> intervalOptionsMinutes = List.of(0, 10, 30, 60);
}
