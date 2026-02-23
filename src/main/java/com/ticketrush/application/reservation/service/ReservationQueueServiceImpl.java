package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.port.outbound.ReservationQueueStatusStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationQueueServiceImpl implements ReservationQueueService {

    private final ReservationQueueStatusStore reservationQueueStatusStore;

    private static final long STATUS_TTL_MINUTES = 30;

    /**
     * 예약 상태 저장 (userId와 seatId 조합을 키로 사용)
     */
    public void setStatus(Long userId, Long seatId, String status) {
        reservationQueueStatusStore.setStatus(userId, seatId, status, STATUS_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 예약 상태 조회
     */
    public String getStatus(Long userId, Long seatId) {
        return reservationQueueStatusStore.getStatus(userId, seatId);
    }
}
