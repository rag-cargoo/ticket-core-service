package com.ticketrush.application.port.outbound;

import java.util.Map;
import java.util.Set;

public interface QueueSubscriberQueryPort {

    Set<Long> getSubscribedQueueUsers(Long concertId);

    Map<Long, Set<Long>> snapshotQueueSubscribers();
}
