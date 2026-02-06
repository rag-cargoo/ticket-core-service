package com.ticketrush.global.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private static final String ACTIVE_KEY_PREFIX = "active-user:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("User-Id");

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "User-Id header is missing");
            return false;
        }

        String activeKey = ACTIVE_KEY_PREFIX + userId;
        Boolean isActive = redisTemplate.hasKey(activeKey);

        if (Boolean.FALSE.equals(isActive)) {
            log.warn(">>>> [Interceptor] 거부된 사용자: {}, 활성 토큰 없음", userId);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not an active user in waiting queue");
            return false;
        }

        log.info(">>>> [Interceptor] 허용된 사용자: {}", userId);
        return true;
    }
}
