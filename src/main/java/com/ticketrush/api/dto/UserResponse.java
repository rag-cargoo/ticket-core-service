package com.ticketrush.api.dto;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserTier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private UserTier tier;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getTier());
    }
}
