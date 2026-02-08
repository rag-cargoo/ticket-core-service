package com.ticketrush.global.config;

import com.ticketrush.global.interceptor.WaitingQueueInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final WaitingQueueInterceptor waitingQueueInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(waitingQueueInterceptor)
                .addPathPatterns("/api/reservations/**", "/api/v1/**", "/api/v2/**", "/api/v3/**", "/api/v4/**")
                .excludePathPatterns("/api/v1/waiting-queue/**");
    }
}
