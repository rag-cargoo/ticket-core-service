package com.ticketrush.api.controller;

import com.ticketrush.api.dto.AgencyResponse;
import com.ticketrush.api.dto.AgencyUpsertRequest;
import com.ticketrush.domain.agency.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;

    @PostMapping
    public ResponseEntity<AgencyResponse> create(@RequestBody AgencyUpsertRequest request) {
        return ResponseEntity.status(201).body(AgencyResponse.from(
                agencyService.create(request.getName(), request.getCountryCode(), request.getHomepageUrl())
        ));
    }

    @GetMapping
    public ResponseEntity<List<AgencyResponse>> getAll() {
        return ResponseEntity.ok(agencyService.getAll().stream()
                .map(AgencyResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgencyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(AgencyResponse.from(agencyService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgencyResponse> update(@PathVariable Long id, @RequestBody AgencyUpsertRequest request) {
        return ResponseEntity.ok(AgencyResponse.from(
                agencyService.update(id, request.getName(), request.getCountryCode(), request.getHomepageUrl())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agencyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
