package com.ticketrush.application.port.outbound;

public interface PushEventPublisherPort {

    void publish(PushEvent event, String key);
}
