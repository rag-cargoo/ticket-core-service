package com.ticketrush.api.dto.reservation;

import com.ticketrush.application.reservation.model.SalesPolicyResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SalesPolicyResponse {
    private Long id;
    private Long concertId;
    private LocalDateTime presaleStartAt;
    private LocalDateTime presaleEndAt;
    private String presaleMinimumTier;
    private LocalDateTime generalSaleStartAt;
    private int maxReservationsPerUser;

    public static SalesPolicyResponse from(SalesPolicyResult result) {
        return new SalesPolicyResponse(
                result.getId(),
                result.getConcertId(),
                result.getPresaleStartAt(),
                result.getPresaleEndAt(),
                result.getPresaleMinimumTier(),
                result.getGeneralSaleStartAt(),
                result.getMaxReservationsPerUser()
        );
    }
}
