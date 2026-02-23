package com.ticketrush.application.auth.port.outbound;

public interface SocialLoginConfigPort {

    String getKakaoClientId();

    String getKakaoClientSecret();

    String getKakaoRedirectUri();

    String getNaverClientId();

    String getNaverClientSecret();

    String getNaverRedirectUri();
}
