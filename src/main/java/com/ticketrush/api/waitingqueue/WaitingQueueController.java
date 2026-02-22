package com.ticketrush.api.waitingqueue;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueRequest;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.application.waitingqueue.model.WaitingQueueJoinCommand;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import com.ticketrush.application.waitingqueue.service.WaitingQueueService;
import com.ticketrush.global.sse.SsePushNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/waiting-queue")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;
    private final SsePushNotifier ssePushNotifier;

    @PostMapping("/join")
    public ResponseEntity<WaitingQueueResponse> join(@RequestBody WaitingQueueRequest request) {
        log.debug(">>>> [Incoming Request] join - userId: {}, concertId: {}", request.getUserId(), request.getConcertId());
        WaitingQueueStatusResult result = waitingQueueService.join(
                new WaitingQueueJoinCommand(request.getUserId(), request.getConcertId())
        );
        return ResponseEntity.ok(toApiResponse(result));
    }

    @GetMapping("/status")
    public ResponseEntity<WaitingQueueResponse> getStatus(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        log.debug(">>>> [Incoming Request] status - userId: {}, concertId: {}", userId, concertId);
        WaitingQueueStatusResult result = waitingQueueService.getStatus(new WaitingQueueStatusQuery(userId, concertId));
        return ResponseEntity.ok(toApiResponse(result));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        log.debug(">>>> [Incoming Request] subscribe - userId: {}, concertId: {}", userId, concertId);

        SseEmitter emitter = ssePushNotifier.subscribeQueue(userId, concertId);
        WaitingQueueStatusResult currentStatus = waitingQueueService.getStatus(new WaitingQueueStatusQuery(userId, concertId));
        Long activeTtlSeconds = currentStatus.getStatus() == WaitingQueueStatusType.ACTIVE
                ? waitingQueueService.getActiveTtlSeconds(userId)
                : 0L;

        WaitingQueueSsePayload payload = WaitingQueueSsePayload.builder()
                .userId(userId)
                .concertId(concertId)
                .status(currentStatus.getStatus().name())
                .rank(currentStatus.getRank())
                .activeTtlSeconds(activeTtlSeconds)
                .timestamp(Instant.now().toString())
                .build();

        if (currentStatus.getStatus() == WaitingQueueStatusType.ACTIVE) {
            ssePushNotifier.sendQueueActivated(userId, concertId, payload);
        } else {
            ssePushNotifier.sendQueueRankUpdate(userId, concertId, payload);
        }

        return emitter;
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
