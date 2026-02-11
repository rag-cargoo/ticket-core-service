package com.ticketrush.domain.auth.model;

import com.ticketrush.domain.user.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialProfile {
    private final SocialProvider provider;
    private final String socialId;
    private final String email;
    private final String displayName;
}
