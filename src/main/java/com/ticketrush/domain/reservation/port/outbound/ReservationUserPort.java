package com.ticketrush.domain.reservation.port.outbound;

import com.ticketrush.domain.user.User;

public interface ReservationUserPort {
    User getUser(Long userId);
}
