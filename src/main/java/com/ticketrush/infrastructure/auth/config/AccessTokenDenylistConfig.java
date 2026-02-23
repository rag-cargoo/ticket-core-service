package com.ticketrush.infrastructure.auth.config;

import com.ticketrush.domain.auth.service.AccessTokenDenylistService;
import com.ticketrush.infrastructure.auth.denylist.InMemoryAccessTokenDenylistService;
import com.ticketrush.infrastructure.auth.denylist.RedisAccessTokenDenylistService;
import com.ticketrush.global.config.AuthJwtProperties;
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
            AuthJwtProperties authJwtProperties
    ) {
        return new RedisAccessTokenDenylistService(redisTemplate, authJwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean(AccessTokenDenylistService.class)
    AccessTokenDenylistService inMemoryAccessTokenDenylistService() {
        return new InMemoryAccessTokenDenylistService();
    }
}
