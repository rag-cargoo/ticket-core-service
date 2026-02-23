package com.ticketrush.api.waitingqueue;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueRequest;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.application.realtime.port.inbound.RealtimeSubscriptionUseCase;
import com.ticketrush.application.waitingqueue.model.WaitingQueueJoinCommand;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.port.inbound.WaitingQueueUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/waiting-queue")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueUseCase waitingQueueUseCase;
    private final RealtimeSubscriptionUseCase realtimeSubscriptionUseCase;

    @PostMapping("/join")
    public ResponseEntity<WaitingQueueResponse> join(@RequestBody WaitingQueueRequest request) {
        log.debug(">>>> [Incoming Request] join - userId: {}, concertId: {}", request.getUserId(), request.getConcertId());
        WaitingQueueStatusResult result = waitingQueueUseCase.join(
                new WaitingQueueJoinCommand(request.getUserId(), request.getConcertId())
        );
        return ResponseEntity.ok(toApiResponse(result));
    }

    @GetMapping("/status")
    public ResponseEntity<WaitingQueueResponse> getStatus(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        log.debug(">>>> [Incoming Request] status - userId: {}, concertId: {}", userId, concertId);
        WaitingQueueStatusResult result = waitingQueueUseCase.getStatus(new WaitingQueueStatusQuery(userId, concertId));
        return ResponseEntity.ok(toApiResponse(result));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        log.debug(">>>> [Incoming Request] subscribe - userId: {}, concertId: {}", userId, concertId);
        return realtimeSubscriptionUseCase.subscribeWaitingQueueSse(userId, concertId);
    }

    private WaitingQueueResponse toApiResponse(WaitingQueueStatusResult result) {
        return WaitingQueueResponse.builder()
                .userId(result.getUserId())
                .concertId(result.getConcertId())
                .status(result.getStatus().name())
                .rank(result.getRank())
                .build();
    }
}
