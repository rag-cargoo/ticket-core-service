package com.ticketrush.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {
    private String code;
    private String state;
}
