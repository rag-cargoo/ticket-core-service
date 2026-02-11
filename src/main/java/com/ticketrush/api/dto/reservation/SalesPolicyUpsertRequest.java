package com.ticketrush.api.dto.reservation;

import com.ticketrush.domain.user.UserTier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SalesPolicyUpsertRequest {
    private LocalDateTime presaleStartAt;
    private LocalDateTime presaleEndAt;
    private UserTier presaleMinimumTier;
    private LocalDateTime generalSaleStartAt;
    private int maxReservationsPerUser;
}
