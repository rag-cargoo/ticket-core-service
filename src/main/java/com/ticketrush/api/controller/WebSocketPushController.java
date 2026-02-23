package com.ticketrush.api.controller;

import com.ticketrush.api.dto.push.WebSocketQueueSubscriptionRequest;
import com.ticketrush.api.dto.push.WebSocketReservationSubscriptionRequest;
import com.ticketrush.api.dto.push.WebSocketSeatMapSubscriptionRequest;
import com.ticketrush.application.auth.model.AuthUserPrincipal;
import com.ticketrush.application.realtime.port.inbound.RealtimeSubscriptionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final RealtimeSubscriptionUseCase realtimeSubscriptionUseCase;

    @PostMapping("/waiting-queue/subscriptions")
    public ResponseEntity<Map<String, String>> subscribeWaitingQueue(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody WebSocketQueueSubscriptionRequest request
    ) {
        Long userId = requiredUserId(principal);
        validateRequestedUserId(request.getUserId(), userId);
        String destination = realtimeSubscriptionUseCase.subscribeQueueWebSocket(userId, request.getConcertId());
        return ResponseEntity.ok(Map.of(
                "transport", "websocket",
                "destination", destination
        ));
    }

    @DeleteMapping("/waiting-queue/subscriptions")
    public ResponseEntity<Void> unsubscribeWaitingQueue(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(required = false) Long userId,
            @RequestParam Long concertId
    ) {
        Long authenticatedUserId = requiredUserId(principal);
        validateRequestedUserId(userId, authenticatedUserId);
        realtimeSubscriptionUseCase.unsubscribeQueueWebSocket(authenticatedUserId, concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservations/subscriptions")
    public ResponseEntity<Map<String, String>> subscribeReservation(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody WebSocketReservationSubscriptionRequest request
    ) {
        Long userId = requiredUserId(principal);
        validateRequestedUserId(request.getUserId(), userId);
        String destination = realtimeSubscriptionUseCase.subscribeReservationWebSocket(userId, request.getSeatId());
        return ResponseEntity.ok(Map.of(
                "transport", "websocket",
                "destination", destination
        ));
    }

    @DeleteMapping("/reservations/subscriptions")
    public ResponseEntity<Void> unsubscribeReservation(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(required = false) Long userId,
            @RequestParam Long seatId
    ) {
        Long authenticatedUserId = requiredUserId(principal);
        validateRequestedUserId(userId, authenticatedUserId);
        realtimeSubscriptionUseCase.unsubscribeReservationWebSocket(authenticatedUserId, seatId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seats/subscriptions")
    public ResponseEntity<Map<String, String>> subscribeSeatMap(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody WebSocketSeatMapSubscriptionRequest request
    ) {
        requiredUserId(principal);
        String destination = realtimeSubscriptionUseCase.subscribeSeatMapWebSocket(request.getOptionId());
        return ResponseEntity.ok(Map.of(
                "transport", "websocket",
                "destination", destination
        ));
    }

    @DeleteMapping("/seats/subscriptions")
    public ResponseEntity<Void> unsubscribeSeatMap(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam Long optionId
    ) {
        requiredUserId(principal);
        realtimeSubscriptionUseCase.unsubscribeSeatMapWebSocket(optionId);
        return ResponseEntity.noContent().build();
    }

    private Long requiredUserId(AuthUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("authenticated user is required");
        }
        return principal.getUserId();
    }

    private void validateRequestedUserId(Long requestedUserId, Long authenticatedUserId) {
        if (requestedUserId == null) {
            return;
        }
        if (!requestedUserId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("requested userId does not match authenticated user");
        }
    }
}
