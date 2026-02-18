package com.ticketrush.global.config;

import com.ticketrush.domain.auth.security.JwtAuthenticationFilter;
import com.ticketrush.global.auth.AuthErrorClassifier;
import com.ticketrush.global.auth.AuthErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/concerts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/concerts/setup").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/concerts/cleanup/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/v1/**", "/api/v2/**", "/api/v3/**", "/api/v4/**").permitAll()
                        .requestMatchers("/api/reservations/v1/**", "/api/reservations/v2/**", "/api/reservations/v3/**",
                                "/api/reservations/v4/**", "/api/reservations/v5/**", "/api/reservations/v6/**").permitAll()
                        .requestMatchers("/api/auth/social/**", "/api/auth/token/refresh").permitAll()
                        .requestMatchers("/api/reservations/v7/audit/abuse").hasRole("ADMIN")
                        .requestMatchers("/api/reservations/v7/**", "/api/auth/me", "/api/auth/logout").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String detail = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR);
                            AuthErrorCode errorCode = AuthErrorClassifier.classifyUnauthorized(
                                    detail,
                                    request.getHeader(HttpHeaders.AUTHORIZATION)
                            );
                            log.warn(
                                    "AUTH_MONITOR code={} status=401 method={} path={} detail={}",
                                    errorCode.name(),
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    detail == null ? "none" : detail
                            );
                            writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, errorCode, "unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            AuthErrorCode errorCode = AuthErrorCode.AUTH_FORBIDDEN;
                            log.warn(
                                    "AUTH_MONITOR code={} status=403 method={} path={} detail={}",
                                    errorCode.name(),
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    "access denied"
                            );
                            writeAuthError(response, HttpServletResponse.SC_FORBIDDEN, errorCode, "forbidden");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}") List<String> allowedOrigins
    ) {
        List<String> normalizedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(normalizedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeAuthError(HttpServletResponse response, int status, AuthErrorCode errorCode, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format(
                        "{\"status\":%d,\"errorCode\":\"%s\",\"message\":\"%s\"}",
                        status,
                        errorCode.name(),
                        message
                )
        );
    }
}
