package com.ticketrush.domain.user;

public enum UserTier {
    BASIC,
    SILVER,
    GOLD,
    VIP;

    public boolean isAtLeast(UserTier requiredTier) {
        if (requiredTier == null) {
            return true;
        }
        return this.ordinal() >= requiredTier.ordinal();
    }
}
