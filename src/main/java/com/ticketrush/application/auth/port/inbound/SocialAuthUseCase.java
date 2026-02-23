package com.ticketrush.application.auth.port.inbound;

import com.ticketrush.application.auth.model.SocialAuthorizeResult;
import com.ticketrush.application.auth.model.SocialLoginUserResult;

public interface SocialAuthUseCase {

    SocialAuthorizeResult getAuthorizeInfo(String provider, String state);

    SocialLoginUserResult login(String provider, String code, String state);
}
