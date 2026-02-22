package com.ticketrush.api.controller;

import com.ticketrush.api.dto.ConcertOptionResponse;
import com.ticketrush.api.dto.ConcertResponse;
import com.ticketrush.api.dto.admin.AdminConcertOptionCreateRequest;
import com.ticketrush.api.dto.admin.AdminConcertOptionUpdateRequest;
import com.ticketrush.api.dto.admin.AdminConcertUpsertRequest;
import com.ticketrush.api.dto.reservation.SalesPolicyResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.service.SalesPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/concerts")
@RequiredArgsConstructor
public class AdminConcertController {

    private final ConcertService concertService;
    private final SalesPolicyService salesPolicyService;

    @PostMapping
    public ResponseEntity<ConcertResponse> create(@RequestBody AdminConcertUpsertRequest request) {
        if (request.getArtistId() != null) {
            return ResponseEntity.status(201).body(ConcertResponse.from(
                    concertService.createConcertByReferences(
                            request.getTitle(),
                            request.getArtistId(),
                            request.getPromoterId(),
                            request.getYoutubeVideoUrl()
                    )
            ));
        }

        return ResponseEntity.status(201).body(ConcertResponse.from(
                concertService.createConcert(
                        request.getTitle(),
                        request.getArtistName(),
                        request.getEntertainmentName(),
                        request.getArtistDisplayName(),
                        request.getArtistGenre(),
                        request.getArtistDebutDate(),
                        request.getEntertainmentCountryCode(),
                        request.getEntertainmentHomepageUrl(),
                        request.getPromoterName(),
                        null,
                        null,
                        request.getYoutubeVideoUrl()
                )
        ));
    }

    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> getById(@PathVariable Long concertId) {
        return ResponseEntity.ok(ConcertResponse.from(concertService.getConcert(concertId)));
    }

    @PutMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> update(
            @PathVariable Long concertId,
            @RequestBody AdminConcertUpsertRequest request
    ) {
        if (request.getArtistId() != null) {
            return ResponseEntity.ok(ConcertResponse.from(
                    concertService.updateConcertByReferences(
                            concertId,
                            request.getTitle(),
                            request.getArtistId(),
                            request.getPromoterId(),
                            request.getYoutubeVideoUrl()
                    )
            ));
        }

        return ResponseEntity.ok(ConcertResponse.from(
                concertService.updateConcert(
                        concertId,
                        request.getTitle(),
                        request.getArtistName(),
                        request.getEntertainmentName(),
                        request.getArtistDisplayName(),
                        request.getArtistGenre(),
                        request.getArtistDebutDate(),
                        request.getEntertainmentCountryCode(),
                        request.getEntertainmentHomepageUrl(),
                        request.getPromoterName(),
                        null,
                        null,
                        request.getYoutubeVideoUrl()
                )
        ));
    }

    @DeleteMapping("/{concertId}")
    public ResponseEntity<Void> delete(@PathVariable Long concertId) {
        concertService.deleteConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{concertId}/options")
    public ResponseEntity<ConcertOptionResponse> createOption(
            @PathVariable Long concertId,
            @RequestBody AdminConcertOptionCreateRequest request
    ) {
        if (request.getConcertDate() == null) {
            throw new IllegalArgumentException("concertDate is required");
        }
        ConcertOptionResponse response = ConcertOptionResponse.from(
                concertService.addOption(
                        concertId,
                        request.getConcertDate(),
                        request.getVenueId(),
                        request.getTicketPriceAmount()
                )
        );
        int seatCount = request.getSeatCount() == null ? 0 : request.getSeatCount();
        if (seatCount > 0) {
            concertService.createSeats(response.getId(), seatCount);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<ConcertOptionResponse> updateOption(
            @PathVariable Long optionId,
            @RequestBody AdminConcertOptionUpdateRequest request
    ) {
        return ResponseEntity.ok(ConcertOptionResponse.from(
                concertService.updateOption(
                        optionId,
                        request.getConcertDate(),
                        request.getVenueId(),
                        request.getTicketPriceAmount()
                )
        ));
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        concertService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{concertId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConcertResponse> uploadThumbnail(
            @PathVariable Long concertId,
            @RequestPart("image") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("thumbnail image is required");
        }
        String contentType = resolveImageContentType(image);
        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to read thumbnail image");
        }
        return ResponseEntity.ok(ConcertResponse.from(concertService.updateThumbnail(concertId, imageBytes, contentType)));
    }

    @DeleteMapping("/{concertId}/thumbnail")
    public ResponseEntity<Void> deleteThumbnail(@PathVariable Long concertId) {
        concertService.deleteThumbnail(concertId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{concertId}/sales-policy")
    public ResponseEntity<SalesPolicyResponse> upsertSalesPolicy(
            @PathVariable Long concertId,
            @RequestBody SalesPolicyUpsertRequest request
    ) {
        return ResponseEntity.ok(SalesPolicyResponse.from(salesPolicyService.upsert(concertId, request)));
    }

    private String resolveImageContentType(MultipartFile image) {
        String raw = image.getContentType();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("thumbnail content type is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(MediaType.IMAGE_JPEG_VALUE)
                && !normalized.equals(MediaType.IMAGE_PNG_VALUE)
                && !normalized.equals("image/webp")) {
            throw new IllegalArgumentException("unsupported thumbnail content type: " + raw);
        }
        return normalized;
    }
}
