package com.ticketrush.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_social_provider_social_id", columnNames = {"social_provider", "social_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", length = 20)
    private SocialProvider socialProvider;

    @Column(name = "social_id", length = 100)
    private String socialId;

    @Column(length = 255)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "wallet_balance_amount", nullable = false)
    private Long walletBalanceAmount;

    public User(String username) {
        this(username, UserTier.BASIC);
    }

    public User(String username, UserTier tier) {
        this(username, tier, UserRole.USER);
    }

    public User(String username, UserTier tier, UserRole role) {
        this.username = username;
        this.tier = tier == null ? UserTier.BASIC : tier;
        this.role = role == null ? UserRole.USER : role;
        this.walletBalanceAmount = 200_000L;
    }

    public static User socialUser(
            String username,
            UserTier tier,
            SocialProvider socialProvider,
            String socialId,
            String email,
            String displayName
    ) {
        return socialUser(username, tier, UserRole.USER, socialProvider, socialId, email, displayName);
    }

    public static User socialUser(
            String username,
            UserTier tier,
            UserRole role,
            SocialProvider socialProvider,
            String socialId,
            String email,
            String displayName
    ) {
        User user = new User(username, tier, role);
        user.socialProvider = socialProvider;
        user.socialId = socialId;
        user.email = email;
        user.displayName = displayName;
        return user;
    }

    public void updateProfile(String username, UserTier tier, String email, String displayName) {
        if (username != null && !username.isBlank()) {
            this.username = username.trim();
        }
        if (tier != null) {
            this.tier = tier;
        }
        if (email != null) {
            this.email = normalizeNullable(email);
        }
        if (displayName != null) {
            this.displayName = normalizeNullable(displayName);
        }
    }

    public void updateSocialProfile(String email, String displayName) {
        this.email = normalizeNullable(email);
        this.displayName = normalizeNullable(displayName);
    }

    public long getWalletBalanceAmountSafe() {
        return walletBalanceAmount == null ? 0L : walletBalanceAmount;
    }

    public void chargeWallet(long amount) {
        validatePositiveAmount(amount);
        this.walletBalanceAmount = getWalletBalanceAmountSafe() + amount;
    }

    public void payFromWallet(long amount) {
        validatePositiveAmount(amount);
        long currentBalance = getWalletBalanceAmountSafe();
        if (currentBalance < amount) {
            throw new IllegalStateException("Insufficient wallet balance.");
        }
        this.walletBalanceAmount = currentBalance - amount;
    }

    private void validatePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
