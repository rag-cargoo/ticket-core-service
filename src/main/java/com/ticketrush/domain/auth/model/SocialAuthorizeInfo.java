package com.ticketrush.domain.auth.model;

import com.ticketrush.domain.user.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialAuthorizeInfo {
    private final SocialProvider provider;
    private final String state;
    private final String authorizeUrl;
}
