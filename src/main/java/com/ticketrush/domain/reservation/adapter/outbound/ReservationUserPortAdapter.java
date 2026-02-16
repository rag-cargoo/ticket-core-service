package com.ticketrush.domain.reservation.adapter.outbound;

import com.ticketrush.domain.reservation.port.outbound.ReservationUserPort;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationUserPortAdapter implements ReservationUserPort {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
