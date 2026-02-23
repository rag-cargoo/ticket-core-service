package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.SalesPolicyResult;
import com.ticketrush.application.reservation.model.SalesPolicyUpsertCommand;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.user.User;

import java.time.LocalDateTime;

public interface SalesPolicyService {
    SalesPolicyResult upsert(Long concertId, SalesPolicyUpsertCommand command);

    SalesPolicyResult getByConcertId(Long concertId);

    void validateHoldRequest(User user, Seat seat, LocalDateTime now);
}
