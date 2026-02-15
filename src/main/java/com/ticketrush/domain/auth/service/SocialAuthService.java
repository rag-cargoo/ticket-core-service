package com.ticketrush.domain.auth.service;

import com.ticketrush.domain.auth.model.SocialAuthorizeInfo;
import com.ticketrush.domain.auth.model.SocialLoginResult;
import com.ticketrush.domain.user.SocialProvider;

public interface SocialAuthService {
    SocialAuthorizeInfo getAuthorizeInfo(SocialProvider provider, String state);

    SocialLoginResult login(SocialProvider provider, String code, String state);
}
