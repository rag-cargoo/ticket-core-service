package com.ticketrush.api.controller;

import com.ticketrush.api.dto.ConcertOptionResponse;
import com.ticketrush.api.dto.ConcertResponse;
import com.ticketrush.api.dto.ConcertSearchPageResponse;
import com.ticketrush.api.dto.ConcertSetupRequest;
import com.ticketrush.api.dto.SeatResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.service.SalesPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;
    private final SalesPolicyService salesPolicyService;

    /**
     * [Admin/Test] 공연 및 좌석 일괄 생성
     */
    @PostMapping("/setup")
    public ResponseEntity<String> setupConcert(@RequestBody ConcertSetupRequest request) {
        var concert = concertService.createConcert(
                request.getTitle(),
                request.getArtistName(),
                request.getEntertainmentName(),
                request.getArtistDisplayName(),
                request.getArtistGenre(),
                request.getArtistDebutDate(),
                request.getEntertainmentCountryCode(),
                request.getEntertainmentHomepageUrl(),
                request.getPromoterName(),
                request.getPromoterCountryCode(),
                request.getPromoterHomepageUrl()
        );
        var option = concertService.addOption(concert.getId(), request.getConcertDate(), null);
        concertService.createSeats(option.getId(), request.getSeatCount());

        return ResponseEntity.ok("Setup completed: ConcertID=" + concert.getId() + ", OptionID=" + option.getId());
    }

    /**
     * [Admin/Test] 테스트 데이터 삭제 (Cleanup)
     */
    @DeleteMapping("/cleanup/{concertId}")
    public ResponseEntity<String> cleanupConcert(@PathVariable Long concertId) {
        concertService.deleteConcert(concertId);
        return ResponseEntity.ok("Cleanup completed for ConcertID: " + concertId);
    }

    /**
     * 전체 공연 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ConcertResponse>> getConcerts() {
        return ResponseEntity.ok(concertService.getConcerts().stream()
                .map(ConcertResponse::from)
                .collect(Collectors.toList()));
    }

    /**
     * 공연 검색/필터/정렬 + 페이징 조회
     */
    @GetMapping("/search")
    public ResponseEntity<ConcertSearchPageResponse> searchConcerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String artistName,
            @RequestParam(required = false) String entertainmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        String[] sortTokens = sort.split(",");
        String sortField = resolveSortField(sortTokens.length > 0 ? sortTokens[0] : "id");
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "asc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = concertService.searchConcerts(keyword, artistName, entertainmentName, pageable)
                .map(ConcertResponse::from);

        return ResponseEntity.ok(ConcertSearchPageResponse.from(result));
    }

    /**
     * 공연의 날짜별 옵션 조회
     */
    @GetMapping("/{id}/options")
    public ResponseEntity<List<ConcertOptionResponse>> getOptions(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getConcertOptions(id).stream()
                .map(ConcertOptionResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{concertId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long concertId) {
        ConcertService.ConcertThumbnail thumbnail = concertService.getThumbnail(concertId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(thumbnail.contentType()))
                .body(thumbnail.bytes());
    }

    /**
     * 특정 공연 옵션의 예약 가능 좌석 조회
     */
    @GetMapping("/options/{optionId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long optionId) {
        return ResponseEntity.ok(concertService.getAvailableSeats(optionId).stream()
                .map(SeatResponse::from)
                .collect(Collectors.toList()));
    }

    /**
     * [Admin/Test] Step 11 - 공연 판매 정책 생성/수정
     */
    @PutMapping("/{concertId}/sales-policy")
    public ResponseEntity<SalesPolicyResponse> upsertSalesPolicy(
            @PathVariable Long concertId,
            @RequestBody SalesPolicyUpsertRequest request
    ) {
        return ResponseEntity.ok(SalesPolicyResponse.from(salesPolicyService.upsert(concertId, request)));
    }

    /**
     * [Read] Step 11 - 공연 판매 정책 조회
     */
    @GetMapping("/{concertId}/sales-policy")
    public ResponseEntity<SalesPolicyResponse> getSalesPolicy(@PathVariable Long concertId) {
        return ResponseEntity.ok(SalesPolicyResponse.from(salesPolicyService.getByConcertId(concertId)));
    }

    private String resolveSortField(String candidate) {
        if ("title".equalsIgnoreCase(candidate)) {
            return "title";
        }
        if ("artistName".equalsIgnoreCase(candidate)) {
            return "artist.name";
        }
        if ("entertainmentName".equalsIgnoreCase(candidate)) {
            return "artist.entertainment.name";
        }
        return "id";
    }

    private Sort.Direction resolveDirection(String candidate) {
        try {
            return Sort.Direction.fromString(candidate);
        } catch (IllegalArgumentException ignored) {
            return Sort.Direction.ASC;
        }
    }
}
