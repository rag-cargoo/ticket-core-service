package com.ticketrush.api.dto;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.SocialProvider;
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
    private UserRole role;
    private SocialProvider socialProvider;
    private String socialId;
    private String email;
    private String displayName;
    private Long walletBalanceAmount;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getTier(),
                user.getRole(),
                user.getSocialProvider(),
                user.getSocialId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getWalletBalanceAmountSafe()
        );
    }
}
