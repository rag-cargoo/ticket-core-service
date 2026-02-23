package com.ticketrush.application.user.model;

import com.ticketrush.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResult {
    private Long id;
    private String username;
    private String tier;
    private String role;
    private String socialProvider;
    private String socialId;
    private String email;
    private String displayName;
    private Long walletBalanceAmount;

    public static UserResult from(User user) {
        return new UserResult(
                user.getId(),
                user.getUsername(),
                user.getTier() == null ? null : user.getTier().name(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getSocialProvider() == null ? null : user.getSocialProvider().name(),
                user.getSocialId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getWalletBalanceAmountSafe()
        );
    }
}
