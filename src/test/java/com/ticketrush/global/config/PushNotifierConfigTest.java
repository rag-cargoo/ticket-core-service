package com.ticketrush.global.config;

import com.ticketrush.global.push.PushNotifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PushNotifierConfigTest {

    private final PushNotifierConfig config = new PushNotifierConfig();

    @Test
    void pushNotifier_shouldSelectSseWhenModeIsSse() {
        PushProperties properties = new PushProperties();
        properties.setMode(PushProperties.Mode.SSE);

        PushNotifier ssePushNotifier = mock(PushNotifier.class);
        PushNotifier webSocketPushNotifier = mock(PushNotifier.class);

        PushNotifier selected = config.pushNotifier(properties, ssePushNotifier, webSocketPushNotifier);

        assertThat(selected).isSameAs(ssePushNotifier);
    }

    @Test
    void pushNotifier_shouldSelectWebSocketWhenModeIsWebsocket() {
        PushProperties properties = new PushProperties();
        properties.setMode(PushProperties.Mode.WEBSOCKET);

        PushNotifier ssePushNotifier = mock(PushNotifier.class);
        PushNotifier webSocketPushNotifier = mock(PushNotifier.class);

        PushNotifier selected = config.pushNotifier(properties, ssePushNotifier, webSocketPushNotifier);

        assertThat(selected).isSameAs(webSocketPushNotifier);
    }
}
