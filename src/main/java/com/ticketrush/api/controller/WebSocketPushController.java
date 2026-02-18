package com.ticketrush.api.controller;

import com.ticketrush.api.dto.push.WebSocketQueueSubscriptionRequest;
import com.ticketrush.api.dto.push.WebSocketReservationSubscriptionRequest;
import com.ticketrush.global.push.WebSocketPushNotifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/push/websocket")
@RequiredArgsConstructor
public class WebSocketPushController {

    private final WebSocketPushNotifier webSocketPushNotifier;

    @PostMapping("/waiting-queue/subscriptions")
    public ResponseEntity<Map<String, String>> subscribeWaitingQueue(
            @Valid @RequestBody WebSocketQueueSubscriptionRequest request
    ) {
        String destination = webSocketPushNotifier.subscribeQueue(request.getUserId(), request.getConcertId());
        return ResponseEntity.ok(Map.of(
                "transport", "websocket",
                "destination", destination
        ));
    }

    @DeleteMapping("/waiting-queue/subscriptions")
    public ResponseEntity<Void> unsubscribeWaitingQueue(
            @RequestParam Long userId,
            @RequestParam Long concertId
    ) {
        webSocketPushNotifier.unsubscribeQueue(userId, concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservations/subscriptions")
    public ResponseEntity<Map<String, String>> subscribeReservation(
            @Valid @RequestBody WebSocketReservationSubscriptionRequest request
    ) {
        String destination = webSocketPushNotifier.subscribeReservation(request.getUserId(), request.getSeatId());
        return ResponseEntity.ok(Map.of(
                "transport", "websocket",
                "destination", destination
        ));
    }

    @DeleteMapping("/reservations/subscriptions")
    public ResponseEntity<Void> unsubscribeReservation(
            @RequestParam Long userId,
            @RequestParam Long seatId
    ) {
        webSocketPushNotifier.unsubscribeReservation(userId, seatId);
        return ResponseEntity.noContent().build();
    }
}
