package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.port.inbound.SalesPolicyUseCase;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.user.User;

import java.time.LocalDateTime;

public interface SalesPolicyService extends SalesPolicyUseCase {

    void validateHoldRequest(User user, Seat seat, LocalDateTime now);
}
