package com.ticketrush.infrastructure.messaging;

import com.ticketrush.domain.reservation.event.ReservationEvent;
import com.ticketrush.domain.reservation.service.ReservationQueueService;
import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.api.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaReservationConsumer {

    private final ReservationService reservationService;
    private final ReservationQueueService queueService;
    private final com.ticketrush.infrastructure.sse.SseEmitterManager sseManager;

    @KafkaListener(topics = "${app.kafka.topic.reservation}", groupId = "${spring.kafka.consumer.group-id:ticket-group}")
    public void consume(ReservationEvent event) {
        log.info("Consumed reservation event - UserId: {}, SeatId: {}, Strategy: {}", 
                event.getUserId(), event.getSeatId(), event.getLockType());

        try {
            queueService.setStatus(event.getUserId(), event.getSeatId(), "PROCESSING");

            ReservationRequest request = new ReservationRequest(event.getUserId(), event.getSeatId());
            
            // 이벤트에 지정된 락 전략에 따라 실제 예약 처리
            if (event.getLockType() == ReservationEvent.LockType.PESSIMISTIC) {
                reservationService.createReservationWithPessimisticLock(request);
            } else {
                reservationService.createReservation(request);
            }

            queueService.setStatus(event.getUserId(), event.getSeatId(), "SUCCESS");
            sseManager.send(event.getUserId(), event.getSeatId(), "SUCCESS");
            log.info("Reservation SUCCESS - UserId: {}, SeatId: {}", event.getUserId(), event.getSeatId());

        } catch (Exception e) {
            log.error("Reservation FAIL - UserId: {}, SeatId: {}, Error: {}", 
                    event.getUserId(), event.getSeatId(), e.getMessage());
            
            String status = "FAIL";
            if (e instanceof IllegalStateException) {
                status = "FAIL_ALREADY_RESERVED";
            } else if (e.getMessage().contains("Optimistic")) {
                status = "FAIL_OPTIMISTIC_CONFLICT";
            } else if (e.getMessage().contains("not found")) {
                status = "FAIL_DATA_NOT_FOUND";
            }
            
            queueService.setStatus(event.getUserId(), event.getSeatId(), status);
            sseManager.send(event.getUserId(), event.getSeatId(), status);
        }
    }
}
