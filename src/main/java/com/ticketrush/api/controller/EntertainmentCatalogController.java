package com.ticketrush.api.controller;

import com.ticketrush.api.dto.EntertainmentResponse;
import com.ticketrush.api.dto.EntertainmentSearchPageResponse;
import com.ticketrush.api.dto.EntertainmentUpsertRequest;
import com.ticketrush.domain.entertainment.service.EntertainmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entertainments")
@RequiredArgsConstructor
public class EntertainmentCatalogController {

    private final EntertainmentService entertainmentService;

    @PostMapping
    public ResponseEntity<EntertainmentResponse> create(@RequestBody EntertainmentUpsertRequest request) {
        return ResponseEntity.status(201).body(EntertainmentResponse.from(
                entertainmentService.create(request.getName(), request.getCountryCode(), request.getHomepageUrl())
        ));
    }

    @GetMapping
    public ResponseEntity<List<EntertainmentResponse>> getAll() {
        return ResponseEntity.ok(entertainmentService.getAll().stream()
                .map(EntertainmentResponse::from)
                .toList());
    }

    @GetMapping("/search")
    public ResponseEntity<EntertainmentSearchPageResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortTokens = sort.split(",");
        String sortField = resolveSortField(sortTokens.length > 0 ? sortTokens[0] : "id");
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "desc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = entertainmentService.search(keyword, pageable).map(EntertainmentResponse::from);
        return ResponseEntity.ok(EntertainmentSearchPageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntertainmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(EntertainmentResponse.from(entertainmentService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntertainmentResponse> update(@PathVariable Long id, @RequestBody EntertainmentUpsertRequest request) {
        return ResponseEntity.ok(EntertainmentResponse.from(
                entertainmentService.update(id, request.getName(), request.getCountryCode(), request.getHomepageUrl())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entertainmentService.delete(id);
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
