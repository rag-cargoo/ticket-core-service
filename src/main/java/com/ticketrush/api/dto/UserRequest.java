package com.ticketrush.api.dto;

import com.ticketrush.domain.user.UserTier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String username;
    private UserTier tier;
}
