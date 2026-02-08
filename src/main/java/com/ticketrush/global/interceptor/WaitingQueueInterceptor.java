package com.ticketrush.global.interceptor;

import com.ticketrush.global.config.WaitingQueueProperties;
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
    private final WaitingQueueProperties waitingQueueProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("User-Id");

        if (userId == null) {
            // 헤더가 없으면 일단 통과 (대기열 진입 API 등을 위해)
            // 실제 검증은 아래 activeKey 체크에서 수행됨
            return true;
        }

        String activeKey = waitingQueueProperties.getActiveKeyPrefix() + userId;
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
