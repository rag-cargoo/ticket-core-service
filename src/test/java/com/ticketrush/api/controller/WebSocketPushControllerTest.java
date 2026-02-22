package com.ticketrush.api.controller;

import com.ticketrush.api.dto.push.WebSocketQueueSubscriptionRequest;
import com.ticketrush.api.dto.push.WebSocketReservationSubscriptionRequest;
import com.ticketrush.api.dto.push.WebSocketSeatMapSubscriptionRequest;
import com.ticketrush.domain.auth.security.AuthUserPrincipal;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.global.push.WebSocketPushNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketPushControllerTest {

    @Mock
    private WebSocketPushNotifier webSocketPushNotifier;

    private WebSocketPushController controller;
    private AuthUserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new WebSocketPushController(webSocketPushNotifier);
        principal = new AuthUserPrincipal(200L, "tester", UserRole.USER);
    }

    @Test
    void subscribeWaitingQueue_usesAuthenticatedUserId() {
        WebSocketQueueSubscriptionRequest request = new WebSocketQueueSubscriptionRequest();
        request.setConcertId(7L);

        when(webSocketPushNotifier.subscribeQueue(200L, 7L))
                .thenReturn("/topic/waiting-queue/7/200");

        ResponseEntity<Map<String, String>> response = controller.subscribeWaitingQueue(principal, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("destination", "/topic/waiting-queue/7/200");
        verify(webSocketPushNotifier).subscribeQueue(eq(200L), eq(7L));
    }

    @Test
    void subscribeWaitingQueue_whenBodyUserIdMismatched_rejectsRequest() {
        WebSocketQueueSubscriptionRequest request = new WebSocketQueueSubscriptionRequest();
        request.setUserId(201L);
        request.setConcertId(7L);

        assertThatThrownBy(() -> controller.subscribeWaitingQueue(principal, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verify(webSocketPushNotifier, never()).subscribeQueue(eq(200L), eq(7L));
    }

    @Test
    void unsubscribeReservation_usesAuthenticatedUserId() {
        ResponseEntity<Void> response = controller.unsubscribeReservation(principal, null, 55L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(webSocketPushNotifier).unsubscribeReservation(200L, 55L);
    }

    @Test
    void subscribeReservation_whenBodyUserIdMatches_allowsRequest() {
        WebSocketReservationSubscriptionRequest request = new WebSocketReservationSubscriptionRequest();
        request.setUserId(200L);
        request.setSeatId(55L);

        when(webSocketPushNotifier.subscribeReservation(200L, 55L))
                .thenReturn("/topic/reservations/55/200");

        ResponseEntity<Map<String, String>> response = controller.subscribeReservation(principal, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("destination", "/topic/reservations/55/200");
        verify(webSocketPushNotifier).subscribeReservation(200L, 55L);
    }

    @Test
    void subscribeSeatMap_returnsTopicDestination() {
        WebSocketSeatMapSubscriptionRequest request = new WebSocketSeatMapSubscriptionRequest();
        request.setOptionId(19L);

        when(webSocketPushNotifier.subscribeSeatMap(19L)).thenReturn("/topic/seats/19");

        ResponseEntity<Map<String, String>> response = controller.subscribeSeatMap(principal, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("destination", "/topic/seats/19");
        verify(webSocketPushNotifier).subscribeSeatMap(19L);
    }

    @Test
    void unsubscribeSeatMap_returnsNoContent() {
        ResponseEntity<Void> response = controller.unsubscribeSeatMap(principal, 19L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(webSocketPushNotifier).unsubscribeSeatMap(19L);
    }
}
