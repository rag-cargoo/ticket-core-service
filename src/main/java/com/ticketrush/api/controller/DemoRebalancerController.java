package com.ticketrush.api.controller;

import com.ticketrush.application.demo.port.inbound.DemoRebalancerUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dev/demo/rebalancer")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-rebalancer", name = "enabled", havingValue = "true")
public class DemoRebalancerController {

    private final DemoRebalancerUseCase demoRebalancerUseCase;

    @GetMapping("/status")
    public ResponseEntity<DemoRebalancerUseCase.DemoRebalancerSnapshot> getStatus() {
        DemoRebalancerUseCase.DemoRebalancerSnapshot snapshot = demoRebalancerUseCase.getSnapshot();
        if (!snapshot.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "demo rebalancer is disabled");
        }
        return ResponseEntity.ok(snapshot);
    }

    @PostMapping("/run")
    public ResponseEntity<DemoRebalancerUseCase.DemoRebalanceTriggerResult> triggerNow() {
        DemoRebalancerUseCase.DemoRebalanceTriggerResult result = demoRebalancerUseCase.triggerNow();
        if (!result.snapshot().enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "demo rebalancer is disabled");
        }
        HttpStatus status = result.accepted() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }
}
