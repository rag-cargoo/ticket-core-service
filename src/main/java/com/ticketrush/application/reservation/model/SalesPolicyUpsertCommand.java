package com.ticketrush.application.reservation.model;

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
    private String presaleMinimumTier;
    private LocalDateTime generalSaleStartAt;
    private int maxReservationsPerUser;
}
