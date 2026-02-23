package com.ticketrush.application.reservation.model;

import com.ticketrush.domain.user.UserTier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SalesPolicyUpsertCommand {
    private LocalDateTime presaleStartAt;
    private LocalDateTime presaleEndAt;
    private UserTier presaleMinimumTier;
    private LocalDateTime generalSaleStartAt;
    private int maxReservationsPerUser;
}
