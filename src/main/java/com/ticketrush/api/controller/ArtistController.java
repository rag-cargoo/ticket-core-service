package com.ticketrush.api.controller;

import com.ticketrush.api.dto.ArtistResponse;
import com.ticketrush.api.dto.ArtistSearchPageResponse;
import com.ticketrush.api.dto.ArtistUpsertRequest;
import com.ticketrush.domain.artist.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @PostMapping
    public ResponseEntity<ArtistResponse> create(@RequestBody ArtistUpsertRequest request) {
        return ResponseEntity.status(201).body(ArtistResponse.from(
                artistService.create(
                        request.getName(),
                        request.getAgencyId(),
                        request.getDisplayName(),
                        request.getGenre(),
                        request.getDebutDate()
                )
        ));
    }

    @GetMapping
    public ResponseEntity<List<ArtistResponse>> getAll() {
        return ResponseEntity.ok(artistService.getAll().stream()
                .map(ArtistResponse::from)
                .toList());
    }

    @GetMapping("/search")
    public ResponseEntity<ArtistSearchPageResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortTokens = sort.split(",");
        String sortField = resolveSortField(sortTokens.length > 0 ? sortTokens[0] : "id");
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "desc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = artistService.search(keyword, agencyId, pageable).map(ArtistResponse::from);
        return ResponseEntity.ok(ArtistSearchPageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ArtistResponse.from(artistService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArtistResponse> update(@PathVariable Long id, @RequestBody ArtistUpsertRequest request) {
        return ResponseEntity.ok(ArtistResponse.from(
                artistService.update(
                        id,
                        request.getName(),
                        request.getAgencyId(),
                        request.getDisplayName(),
                        request.getGenre(),
                        request.getDebutDate()
                )
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveSortField(String candidate) {
        if ("name".equalsIgnoreCase(candidate)) {
            return "name";
        }
        if ("displayName".equalsIgnoreCase(candidate)) {
            return "displayName";
        }
        if ("genre".equalsIgnoreCase(candidate)) {
            return "genre";
        }
        if ("debutDate".equalsIgnoreCase(candidate)) {
            return "debutDate";
        }
        if ("agencyName".equalsIgnoreCase(candidate)) {
            return "agencyName";
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
