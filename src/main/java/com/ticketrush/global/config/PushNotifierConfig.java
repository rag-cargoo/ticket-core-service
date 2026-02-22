package com.ticketrush.global.config;

import com.ticketrush.global.push.PushNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushNotifierConfig {

    @Bean
    @Primary
    public PushNotifier pushNotifier(
            PushProperties properties,
            @Qualifier("ssePushNotifier") PushNotifier ssePushNotifier,
            @Qualifier("kafkaWebSocketPushNotifier") PushNotifier kafkaWebSocketPushNotifier
    ) {
        if (properties.getMode() == PushProperties.Mode.WEBSOCKET) {
            log.info("Push notifier mode: WEBSOCKET (Kafka fanout)");
            return kafkaWebSocketPushNotifier;
        }

        log.info("Push notifier mode: SSE");
        return ssePushNotifier;
    }
}
