package com.ticketrush.application.reservation.model;

import com.ticketrush.domain.reservation.entity.SalesPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SalesPolicyResult {
    private Long id;
    private Long concertId;
    private LocalDateTime presaleStartAt;
    private LocalDateTime presaleEndAt;
    private String presaleMinimumTier;
    private LocalDateTime generalSaleStartAt;
    private int maxReservationsPerUser;

    public static SalesPolicyResult from(SalesPolicy policy) {
        return new SalesPolicyResult(
                policy.getId(),
                policy.getConcert().getId(),
                policy.getPresaleStartAt(),
                policy.getPresaleEndAt(),
                policy.getPresaleMinimumTier() == null ? null : policy.getPresaleMinimumTier().name(),
                policy.getGeneralSaleStartAt(),
                policy.getMaxReservationsPerUser()
        );
    }
}
