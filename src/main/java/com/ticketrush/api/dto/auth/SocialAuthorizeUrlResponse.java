package com.ticketrush.api.dto.auth;

import com.ticketrush.application.auth.model.SocialAuthorizeResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialAuthorizeUrlResponse {
    private String provider;
    private String state;
    private String authorizeUrl;

    public static SocialAuthorizeUrlResponse from(SocialAuthorizeResult info) {
        return new SocialAuthorizeUrlResponse(
                info.getProvider(),
                info.getState(),
                info.getAuthorizeUrl()
        );
    }
}
