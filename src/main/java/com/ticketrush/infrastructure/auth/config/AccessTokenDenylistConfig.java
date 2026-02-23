package com.ticketrush.infrastructure.auth.config;

import com.ticketrush.application.auth.port.outbound.AuthJwtConfigPort;
import com.ticketrush.domain.auth.service.AccessTokenDenylistService;
import com.ticketrush.infrastructure.auth.denylist.InMemoryAccessTokenDenylistService;
import com.ticketrush.infrastructure.auth.denylist.RedisAccessTokenDenylistService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AccessTokenDenylistConfig {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    AccessTokenDenylistService redisAccessTokenDenylistService(
            StringRedisTemplate redisTemplate,
            AuthJwtConfigPort authJwtConfigPort
    ) {
        return new RedisAccessTokenDenylistService(redisTemplate, authJwtConfigPort);
    }

    @Bean
    @ConditionalOnMissingBean(AccessTokenDenylistService.class)
    AccessTokenDenylistService inMemoryAccessTokenDenylistService() {
        return new InMemoryAccessTokenDenylistService();
    }
}
