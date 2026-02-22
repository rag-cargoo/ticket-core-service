package com.ticketrush.api.controller;

import com.ticketrush.api.dto.PromoterResponse;
import com.ticketrush.api.dto.PromoterSearchPageResponse;
import com.ticketrush.api.dto.PromoterUpsertRequest;
import com.ticketrush.domain.promoter.service.PromoterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/promoters")
@RequiredArgsConstructor
public class PromoterController {

    private final PromoterService promoterService;

    @PostMapping
    public ResponseEntity<PromoterResponse> create(@RequestBody PromoterUpsertRequest request) {
        return ResponseEntity.status(201).body(PromoterResponse.from(
                promoterService.create(request.getName(), request.getCountryCode(), request.getHomepageUrl())
        ));
    }

    @GetMapping
    public ResponseEntity<List<PromoterResponse>> getAll() {
        return ResponseEntity.ok(promoterService.getAll().stream()
                .map(PromoterResponse::from)
                .toList());
    }

    @GetMapping("/search")
    public ResponseEntity<PromoterSearchPageResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortTokens = sort.split(",");
        String sortField = resolveSortField(sortTokens.length > 0 ? sortTokens[0] : "id");
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "desc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = promoterService.search(keyword, pageable).map(PromoterResponse::from);
        return ResponseEntity.ok(PromoterSearchPageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(PromoterResponse.from(promoterService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromoterResponse> update(@PathVariable Long id, @RequestBody PromoterUpsertRequest request) {
        return ResponseEntity.ok(PromoterResponse.from(
                promoterService.update(id, request.getName(), request.getCountryCode(), request.getHomepageUrl())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promoterService.delete(id);
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
