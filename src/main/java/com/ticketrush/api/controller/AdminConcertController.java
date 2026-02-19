package com.ticketrush.api.controller;

import com.ticketrush.api.dto.ConcertOptionResponse;
import com.ticketrush.api.dto.ConcertResponse;
import com.ticketrush.api.dto.admin.AdminConcertOptionCreateRequest;
import com.ticketrush.api.dto.admin.AdminConcertOptionUpdateRequest;
import com.ticketrush.api.dto.admin.AdminConcertUpsertRequest;
import com.ticketrush.api.dto.reservation.SalesPolicyResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.service.SalesPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/concerts")
@RequiredArgsConstructor
public class AdminConcertController {

    private static final int THUMBNAIL_WIDTH = 640;
    private static final int THUMBNAIL_HEIGHT = 360;
    private static final long MAX_THUMBNAIL_UPLOAD_BYTES = 5L * 1024L * 1024L;

    private final ConcertService concertService;
    private final SalesPolicyService salesPolicyService;

    @PostMapping
    public ResponseEntity<ConcertResponse> createConcert(@RequestBody AdminConcertUpsertRequest request) {
        Concert concert = concertService.createConcert(
                request.getTitle(),
                request.getArtistName(),
                request.getAgencyName(),
                request.getArtistDisplayName(),
                request.getArtistGenre(),
                request.getArtistDebutDate(),
                request.getAgencyCountryCode(),
                request.getAgencyHomepageUrl(),
                request.getYoutubeVideoUrl()
        );
        return ResponseEntity.status(201).body(toConcertResponse(concert));
    }

    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        return ResponseEntity.ok(toConcertResponse(concertService.getConcert(concertId)));
    }

    @PutMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> updateConcert(
            @PathVariable Long concertId,
            @RequestBody AdminConcertUpsertRequest request
    ) {
        Concert concert = concertService.updateConcert(
                concertId,
                request.getTitle(),
                request.getArtistName(),
                request.getAgencyName(),
                request.getArtistDisplayName(),
                request.getArtistGenre(),
                request.getArtistDebutDate(),
                request.getAgencyCountryCode(),
                request.getAgencyHomepageUrl(),
                request.getYoutubeVideoUrl()
        );
        return ResponseEntity.ok(toConcertResponse(concert));
    }

    @DeleteMapping("/{concertId}")
    public ResponseEntity<Void> deleteConcert(@PathVariable Long concertId) {
        concertService.deleteConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{concertId}/options")
    public ResponseEntity<ConcertOptionResponse> createOption(
            @PathVariable Long concertId,
            @RequestBody AdminConcertOptionCreateRequest request
    ) {
        int seatCount = normalizeSeatCount(request.getSeatCount());
        var option = concertService.addOption(concertId, request.getConcertDate(), request.getTicketPriceAmount());
        concertService.createSeats(option.getId(), seatCount);
        return ResponseEntity.status(201).body(ConcertOptionResponse.from(option));
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<ConcertOptionResponse> updateOption(
            @PathVariable Long optionId,
            @RequestBody AdminConcertOptionUpdateRequest request
    ) {
        var option = concertService.updateOption(optionId, request.getConcertDate(), request.getTicketPriceAmount());
        return ResponseEntity.ok(ConcertOptionResponse.from(option));
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        concertService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{concertId}/sales-policy")
    public ResponseEntity<SalesPolicyResponse> upsertSalesPolicy(
            @PathVariable Long concertId,
            @RequestBody SalesPolicyUpsertRequest request
    ) {
        return ResponseEntity.ok(SalesPolicyResponse.from(salesPolicyService.upsert(concertId, request)));
    }

    @PostMapping(value = "/{concertId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConcertResponse> uploadThumbnail(
            @PathVariable Long concertId,
            @RequestParam("image") MultipartFile image
    ) {
        ThumbnailPayload payload = generateThumbnailPayload(image);
        Concert concert = concertService.updateConcertThumbnail(
                concertId,
                payload.originalFilename(),
                payload.originalContentType(),
                payload.originalBytes(),
                payload.thumbnailContentType(),
                payload.thumbnailBytes()
        );
        return ResponseEntity.ok(toConcertResponse(concert));
    }

    @DeleteMapping("/{concertId}/thumbnail")
    public ResponseEntity<Void> deleteThumbnail(@PathVariable Long concertId) {
        concertService.clearConcertThumbnail(concertId);
        return ResponseEntity.noContent().build();
    }

    private ConcertResponse toConcertResponse(Concert concert) {
        Map<Long, com.ticketrush.domain.concert.service.ConcertSaleSnapshot> snapshotMap =
                concertService.getConcertSaleSnapshots(List.of(concert.getId()), LocalDateTime.now());
        return ConcertResponse.from(concert, snapshotMap.get(concert.getId()));
    }

    private int normalizeSeatCount(Integer seatCount) {
        if (seatCount == null || seatCount < 1) {
            throw new IllegalArgumentException("seatCount must be >= 1");
        }
        return seatCount;
    }

    private ThumbnailPayload generateThumbnailPayload(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("thumbnail image is required");
        }
        if (image.getSize() > MAX_THUMBNAIL_UPLOAD_BYTES) {
            throw new IllegalArgumentException("thumbnail image size must be <= 5MB");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("thumbnail image content type must be image/*");
        }

        byte[] originalBytes = readBytes(image);
        BufferedImage sourceImage = readImage(originalBytes);
        byte[] thumbnailBytes = renderThumbnailJpeg(sourceImage, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        return new ThumbnailPayload(
                image.getOriginalFilename(),
                contentType,
                originalBytes,
                MediaType.IMAGE_JPEG_VALUE,
                thumbnailBytes
        );
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to read thumbnail image bytes");
        }
    }

    private BufferedImage readImage(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IllegalArgumentException("thumbnail image is invalid");
            }
            return bufferedImage;
        } catch (IOException e) {
            throw new IllegalArgumentException("thumbnail image is invalid");
        }
    }

    private byte[] renderThumbnailJpeg(BufferedImage source, int targetWidth, int targetHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("thumbnail image dimensions are invalid");
        }

        double scale = Math.max((double) targetWidth / sourceWidth, (double) targetHeight / sourceHeight);
        int scaledWidth = (int) Math.ceil(sourceWidth * scale);
        int scaledHeight = (int) Math.ceil(sourceHeight * scale);
        int offsetX = (targetWidth - scaledWidth) / 2;
        int offsetY = (targetHeight - scaledHeight) / 2;

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.drawImage(source, offsetX, offsetY, scaledWidth, scaledHeight, null);
        graphics.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(target, "jpg", outputStream)) {
                throw new IllegalArgumentException("thumbnail image encoding failed");
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("thumbnail image encoding failed");
        }
    }

    private record ThumbnailPayload(
            String originalFilename,
            String originalContentType,
            byte[] originalBytes,
            String thumbnailContentType,
            byte[] thumbnailBytes
    ) {
    }
}
