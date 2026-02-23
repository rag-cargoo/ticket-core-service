package com.ticketrush.application.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialAuthorizeResult {

    private final String provider;
    private final String state;
    private final String authorizeUrl;
}
