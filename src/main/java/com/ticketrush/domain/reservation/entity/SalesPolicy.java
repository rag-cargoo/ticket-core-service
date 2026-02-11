package com.ticketrush.domain.reservation.entity;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.user.UserTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sales_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false, unique = true)
    private Concert concert;

    private LocalDateTime presaleStartAt;

    private LocalDateTime presaleEndAt;

    @Enumerated(EnumType.STRING)
    private UserTier presaleMinimumTier;

    @Column(nullable = false)
    private LocalDateTime generalSaleStartAt;

    @Column(nullable = false)
    private int maxReservationsPerUser;

    private SalesPolicy(
            Concert concert,
            LocalDateTime presaleStartAt,
            LocalDateTime presaleEndAt,
            UserTier presaleMinimumTier,
            LocalDateTime generalSaleStartAt,
            int maxReservationsPerUser
    ) {
        this.concert = concert;
        apply(presaleStartAt, presaleEndAt, presaleMinimumTier, generalSaleStartAt, maxReservationsPerUser);
    }

    public static SalesPolicy create(
            Concert concert,
            LocalDateTime presaleStartAt,
            LocalDateTime presaleEndAt,
            UserTier presaleMinimumTier,
            LocalDateTime generalSaleStartAt,
            int maxReservationsPerUser
    ) {
        return new SalesPolicy(concert, presaleStartAt, presaleEndAt, presaleMinimumTier, generalSaleStartAt, maxReservationsPerUser);
    }

    public void update(
            LocalDateTime presaleStartAt,
            LocalDateTime presaleEndAt,
            UserTier presaleMinimumTier,
            LocalDateTime generalSaleStartAt,
            int maxReservationsPerUser
    ) {
        apply(presaleStartAt, presaleEndAt, presaleMinimumTier, generalSaleStartAt, maxReservationsPerUser);
    }

    public void validateHoldRequest(UserTier userTier, LocalDateTime now) {
        if (isInPresaleWindow(now)) {
            if (presaleMinimumTier != null && !userTier.isAtLeast(presaleMinimumTier)) {
                throw new IllegalStateException("Presale tier not eligible. userTier=" + userTier + ", required=" + presaleMinimumTier);
            }
            return;
        }

        if (now.isBefore(generalSaleStartAt)) {
            throw new IllegalStateException("Sale has not opened yet. generalSaleStartAt=" + generalSaleStartAt);
        }
    }

    private boolean isInPresaleWindow(LocalDateTime now) {
        if (presaleStartAt == null || presaleEndAt == null) {
            return false;
        }
        return !now.isBefore(presaleStartAt) && now.isBefore(presaleEndAt);
    }

    private void apply(
            LocalDateTime presaleStartAt,
            LocalDateTime presaleEndAt,
            UserTier presaleMinimumTier,
            LocalDateTime generalSaleStartAt,
            int maxReservationsPerUser
    ) {
        validatePolicyRange(presaleStartAt, presaleEndAt, generalSaleStartAt, maxReservationsPerUser);
        this.presaleStartAt = presaleStartAt;
        this.presaleEndAt = presaleEndAt;
        this.presaleMinimumTier = presaleMinimumTier;
        this.generalSaleStartAt = generalSaleStartAt;
        this.maxReservationsPerUser = maxReservationsPerUser;
    }

    private void validatePolicyRange(
            LocalDateTime presaleStartAt,
            LocalDateTime presaleEndAt,
            LocalDateTime generalSaleStartAt,
            int maxReservationsPerUser
    ) {
        if (generalSaleStartAt == null) {
            throw new IllegalArgumentException("generalSaleStartAt is required.");
        }
        if (maxReservationsPerUser < 1) {
            throw new IllegalArgumentException("maxReservationsPerUser must be >= 1.");
        }
        if ((presaleStartAt == null) != (presaleEndAt == null)) {
            throw new IllegalArgumentException("presaleStartAt and presaleEndAt must be both null or both set.");
        }
        if (presaleStartAt != null) {
            if (!presaleStartAt.isBefore(presaleEndAt)) {
                throw new IllegalArgumentException("presaleStartAt must be before presaleEndAt.");
            }
            if (presaleEndAt.isAfter(generalSaleStartAt)) {
                throw new IllegalArgumentException("presaleEndAt must be before or equal to generalSaleStartAt.");
            }
        }
    }
}
