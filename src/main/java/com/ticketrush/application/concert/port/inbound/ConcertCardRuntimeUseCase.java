package com.ticketrush.application.concert.port.inbound;

import com.ticketrush.application.concert.model.ConcertCardRuntimeSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ConcertCardRuntimeUseCase {

    List<Long> findAllConcertIds();

    Map<Long, ConcertCardRuntimeSnapshot> resolveSnapshots(List<Long> concertIds, Instant serverNow);
}
