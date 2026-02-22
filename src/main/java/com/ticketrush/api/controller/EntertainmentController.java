package com.ticketrush.api.controller;

import com.ticketrush.api.dto.AgencyResponse;
import com.ticketrush.api.dto.AgencySearchPageResponse;
import com.ticketrush.api.dto.AgencyUpsertRequest;
import com.ticketrush.domain.agency.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/entertainments")
@RequiredArgsConstructor
public class EntertainmentController {

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

    @GetMapping("/search")
    public ResponseEntity<AgencySearchPageResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortTokens = sort.split(",");
        String sortField = resolveSortField(sortTokens.length > 0 ? sortTokens[0] : "id");
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "desc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = agencyService.search(keyword, pageable).map(AgencyResponse::from);
        return ResponseEntity.ok(AgencySearchPageResponse.from(result));
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

    private String resolveSortField(String candidate) {
        if ("name".equalsIgnoreCase(candidate)) {
            return "name";
        }
        if ("countryCode".equalsIgnoreCase(candidate)) {
            return "countryCode";
        }
        return "id";
    }

    private Sort.Direction resolveDirection(String candidate) {
        try {
            return Sort.Direction.fromString(candidate);
        } catch (IllegalArgumentException ignored) {
            return Sort.Direction.DESC;
        }
    }
}
