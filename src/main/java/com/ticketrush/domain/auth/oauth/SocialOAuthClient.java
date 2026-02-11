package com.ticketrush.domain.auth.oauth;

import com.ticketrush.domain.auth.model.SocialProfile;
import com.ticketrush.domain.user.SocialProvider;

public interface SocialOAuthClient {
    SocialProvider provider();

    String buildAuthorizeUrl(String state);

    SocialProfile fetchProfile(String code, String state);
}
