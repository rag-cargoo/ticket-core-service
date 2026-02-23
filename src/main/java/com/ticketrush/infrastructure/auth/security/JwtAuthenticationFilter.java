package com.ticketrush.infrastructure.auth.security;

import com.ticketrush.application.auth.model.AuthUserPrincipal;
import com.ticketrush.application.auth.port.inbound.AuthTokenAuthenticationUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_MESSAGE_ATTR = "auth.error.message";

    private final AuthTokenAuthenticationUseCase authTokenAuthenticationUseCase;

    public JwtAuthenticationFilter(
            AuthTokenAuthenticationUseCase authTokenAuthenticationUseCase
    ) {
        this.authTokenAuthenticationUseCase = authTokenAuthenticationUseCase;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String bearerToken = resolveBearerToken(request);
        if (bearerToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthUserPrincipal principal = authTokenAuthenticationUseCase.authenticateAccessToken(bearerToken);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name()))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException e) {
                request.setAttribute(AUTH_ERROR_MESSAGE_ATTR, e.getMessage());
                // invalid token: leave context empty and let security entry point handle protected paths
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7).trim();
    }
}
