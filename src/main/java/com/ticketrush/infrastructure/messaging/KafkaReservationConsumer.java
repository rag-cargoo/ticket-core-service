package com.ticketrush.infrastructure.messaging;

import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationQueueEvent;
import com.ticketrush.application.reservation.model.ReservationQueueLockType;
import com.ticketrush.application.reservation.port.inbound.ReservationQueueRuntimeUseCase;
import com.ticketrush.application.reservation.port.inbound.ReservationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaReservationConsumer {

    private final ReservationUseCase reservationUseCase;
    private final ReservationQueueRuntimeUseCase queueRuntimeUseCase;
    private final ReservationStatusPushPort pushNotifier;

    @KafkaListener(topics = "${app.kafka.topic.reservation}", groupId = "${spring.kafka.consumer.group-id:ticket-group}")
    public void consume(ReservationQueueEvent event) {
        log.info("Consumed reservation event - UserId: {}, SeatId: {}, Strategy: {}", 
                event.getUserId(), event.getSeatId(), event.getLockType());

        try {
            queueRuntimeUseCase.setStatus(event.getUserId(), event.getSeatId(), "PROCESSING");

            ReservationCreateCommand command = new ReservationCreateCommand(event.getUserId(), event.getSeatId(), null, null);
            
            // 이벤트에 지정된 락 전략에 따라 실제 예약 처리
            if (event.getLockType() == ReservationQueueLockType.PESSIMISTIC) {
                reservationUseCase.createReservationWithPessimisticLock(command);
            } else {
                reservationUseCase.createReservation(command);
            }

            queueRuntimeUseCase.setStatus(event.getUserId(), event.getSeatId(), "SUCCESS");
            pushNotifier.sendReservationStatus(event.getUserId(), event.getSeatId(), "SUCCESS");
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
            
            queueRuntimeUseCase.setStatus(event.getUserId(), event.getSeatId(), status);
            pushNotifier.sendReservationStatus(event.getUserId(), event.getSeatId(), status);
        }
    }
}
