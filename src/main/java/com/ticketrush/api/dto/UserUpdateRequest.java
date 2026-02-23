package com.ticketrush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String username;
    private String tier;
    private String email;
    private String displayName;
}
