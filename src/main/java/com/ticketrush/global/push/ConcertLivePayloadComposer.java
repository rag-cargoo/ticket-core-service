package com.ticketrush.global.push;

import com.ticketrush.application.concert.model.ConcertCardRuntimeSnapshot;
import com.ticketrush.application.concert.model.ConcertLiveCardPayload;
import com.ticketrush.application.concert.model.ConcertResult;
import com.ticketrush.application.concert.port.inbound.ConcertCardRuntimeUseCase;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ConcertLivePayloadComposer {

    private final ConcertRepository concertRepository;
    private final ConcertCardRuntimeUseCase concertCardRuntimeUseCase;

    @Transactional(readOnly = true)
    public List<ConcertLiveCardPayload> compose(Instant serverNow) {
        List<Concert> concerts = concertRepository.findAllForLivePayload().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Concert::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (concerts.isEmpty()) {
            return List.of();
        }

        List<Long> concertIds = concerts.stream()
                .map(Concert::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, ConcertCardRuntimeSnapshot> runtimeSnapshotMap = concertCardRuntimeUseCase.resolveSnapshots(concertIds, serverNow);

        return concerts.stream()
                .map(concert -> {
                    ConcertResult result = ConcertResult.from(concert);
                    return ConcertLiveCardPayload.from(result, runtimeSnapshotMap.get(result.getId()));
                })
                .toList();
    }
}
