package com.ticketrush.api.dto.auth;

import com.ticketrush.domain.auth.model.SocialAuthorizeInfo;
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

    public static SocialAuthorizeUrlResponse from(SocialAuthorizeInfo info) {
        return new SocialAuthorizeUrlResponse(
                info.getProvider().name().toLowerCase(),
                info.getState(),
                info.getAuthorizeUrl()
        );
    }
}
