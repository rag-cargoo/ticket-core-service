package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.jwt")
public class AuthJwtProperties {
    private String issuer = "ticketrush";
    private String secret;
    private long accessTokenSeconds = 1800;
    private long refreshTokenSeconds = 1209600;
}
