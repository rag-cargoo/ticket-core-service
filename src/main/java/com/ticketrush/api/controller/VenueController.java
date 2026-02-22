package com.ticketrush.api.controller;

import com.ticketrush.api.dto.VenueResponse;
import com.ticketrush.api.dto.VenueSearchPageResponse;
import com.ticketrush.api.dto.VenueUpsertRequest;
import com.ticketrush.domain.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueResponse> create(@RequestBody VenueUpsertRequest request) {
        return ResponseEntity.status(201).body(VenueResponse.from(
                venueService.create(
                        request.getName(),
                        request.getCity(),
                        request.getCountryCode(),
                        request.getAddress()
                )
        ));
    }

    @GetMapping
    public ResponseEntity<List<VenueResponse>> getAll() {
        return ResponseEntity.ok(venueService.getAll().stream()
                .map(VenueResponse::from)
                .toList());
    }

    @GetMapping("/search")
    public ResponseEntity<VenueSearchPageResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortTokens = sort.split(",");
        String sortField = resolveSortField(sortTokens.length > 0 ? sortTokens[0] : "id");
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "desc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = venueService.search(keyword, pageable).map(VenueResponse::from);
        return ResponseEntity.ok(VenueSearchPageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(VenueResponse.from(venueService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueResponse> update(@PathVariable Long id, @RequestBody VenueUpsertRequest request) {
        return ResponseEntity.ok(VenueResponse.from(
                venueService.update(
                        id,
                        request.getName(),
                        request.getCity(),
                        request.getCountryCode(),
                        request.getAddress()
                )
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        venueService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveSortField(String candidate) {
        if ("name".equalsIgnoreCase(candidate)) {
            return "name";
        }
        if ("city".equalsIgnoreCase(candidate)) {
            return "city";
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
