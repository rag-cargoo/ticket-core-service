package com.ticketrush.global.config;

import com.ticketrush.application.auth.port.outbound.SocialLoginConfigPort;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.social")
public class SocialLoginProperties implements SocialLoginConfigPort {

    private Provider kakao = new Provider();
    private Provider naver = new Provider();
    private RealE2e realE2e = new RealE2e();

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String serviceUrl;
    }

    @Getter
    @Setter
    public static class RealE2e {
        private boolean enabled = false;
    }

    @Override
    public String getKakaoClientId() {
        return kakao.getClientId();
    }

    @Override
    public String getKakaoClientSecret() {
        return kakao.getClientSecret();
    }

    @Override
    public String getKakaoRedirectUri() {
        return kakao.getRedirectUri();
    }

    @Override
    public String getNaverClientId() {
        return naver.getClientId();
    }

    @Override
    public String getNaverClientSecret() {
        return naver.getClientSecret();
    }

    @Override
    public String getNaverRedirectUri() {
        return naver.getRedirectUri();
    }
}
