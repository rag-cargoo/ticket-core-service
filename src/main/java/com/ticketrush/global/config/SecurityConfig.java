package com.ticketrush.global.config;

import com.ticketrush.domain.auth.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":401,\"message\":\"unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":403,\"message\":\"forbidden\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
