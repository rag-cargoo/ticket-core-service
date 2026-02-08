package com.ticketrush.api.waitingqueue;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueRequest;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import com.ticketrush.global.sse.SseEmitterManager;
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
    private final SseEmitterManager sseEmitterManager;

    @PostMapping("/join")
    public ResponseEntity<WaitingQueueResponse> join(@RequestBody WaitingQueueRequest request) {
        log.info(">>>> [Incoming Request] join - userId: {}, concertId: {}", request.getUserId(), request.getConcertId());
        return ResponseEntity.ok(waitingQueueService.join(request.getUserId(), request.getConcertId()));
    }

    @GetMapping("/status")
    public ResponseEntity<WaitingQueueResponse> getStatus(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        log.info(">>>> [Incoming Request] status - userId: {}, concertId: {}", userId, concertId);
        return ResponseEntity.ok(waitingQueueService.getStatus(userId, concertId));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        log.info(">>>> [Incoming Request] subscribe - userId: {}, concertId: {}", userId, concertId);

        SseEmitter emitter = sseEmitterManager.subscribeQueue(userId, concertId);
        WaitingQueueResponse currentStatus = waitingQueueService.getStatus(userId, concertId);
        Long activeTtlSeconds = WaitingQueueStatus.ACTIVE.name().equals(currentStatus.getStatus())
                ? waitingQueueService.getActiveTtlSeconds(userId)
                : 0L;

        WaitingQueueSsePayload payload = WaitingQueueSsePayload.builder()
                .userId(userId)
                .concertId(concertId)
                .status(currentStatus.getStatus())
                .rank(currentStatus.getRank())
                .activeTtlSeconds(activeTtlSeconds)
                .timestamp(Instant.now().toString())
                .build();

        if (WaitingQueueStatus.ACTIVE.name().equals(currentStatus.getStatus())) {
            sseEmitterManager.sendQueueActivated(userId, concertId, payload);
        } else {
            sseEmitterManager.sendQueueRankUpdate(userId, concertId, payload);
        }

        return emitter;
    }
}
