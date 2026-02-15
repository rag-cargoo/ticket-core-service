package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.user.User;

import java.time.LocalDateTime;

public interface SalesPolicyService {
    SalesPolicy upsert(Long concertId, SalesPolicyUpsertRequest request);

    SalesPolicy getByConcertId(Long concertId);

    void validateHoldRequest(User user, Seat seat, LocalDateTime now);
}
