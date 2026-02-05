package com.ticketrush.api.waitingqueue;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueRequest;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/waiting-queue")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    @PostMapping("/join")
    public ResponseEntity<WaitingQueueResponse> join(@RequestBody WaitingQueueRequest request) {
        return ResponseEntity.ok(waitingQueueService.join(request.getUserId(), request.getConcertId()));
    }

    @GetMapping("/status")
    public ResponseEntity<WaitingQueueResponse> getStatus(
            @RequestParam Long userId,
            @RequestParam Long concertId) {
        return ResponseEntity.ok(waitingQueueService.getStatus(userId, concertId));
    }
}
