package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.user.User;

public record UserRequest(String username) {}

public record UserResponse(Long id, String username) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername());
    }
}
