package com.ticketrush.application.auth.port.inbound;

import com.ticketrush.domain.auth.model.SocialAuthorizeInfo;
import com.ticketrush.domain.auth.model.SocialLoginResult;
import com.ticketrush.domain.user.SocialProvider;

public interface SocialAuthUseCase {

    SocialAuthorizeInfo getAuthorizeInfo(SocialProvider provider, String state);

    SocialLoginResult login(SocialProvider provider, String code, String state);
}
