package com.ticketrush.api.controller;

import com.ticketrush.application.reservation.model.SalesPolicyUpsertCommand;
import com.ticketrush.application.concert.port.inbound.ConcertUseCase;
import com.ticketrush.api.dto.ConcertOptionResponse;
import com.ticketrush.api.dto.ConcertResponse;
import com.ticketrush.api.dto.ConcertHighlightsResponse;
import com.ticketrush.api.dto.ConcertSearchPageResponse;
import com.ticketrush.api.dto.ConcertSetupRequest;
import com.ticketrush.api.dto.SeatResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.application.reservation.port.inbound.SalesPolicyUseCase;
import com.ticketrush.domain.venue.Venue;
import com.ticketrush.domain.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertUseCase concertUseCase;
    private final SalesPolicyUseCase salesPolicyUseCase;
    private final VenueRepository venueRepository;

    /**
     * [Admin/Test] 공연 및 좌석 일괄 생성
     */
    @PostMapping("/setup")
    public ResponseEntity<String> setupConcert(@RequestBody ConcertSetupRequest request) {
        var concert = concertUseCase.createConcertResult(
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
                request.getPromoterHomepageUrl(),
                request.getYoutubeVideoUrl()
        );
        Long venueId = resolveVenueId(request);
        int optionCount = Math.max(1, request.getOptionCount());
        List<Long> optionIds = new ArrayList<>(optionCount);
        for (int index = 0; index < optionCount; index++) {
            var option = concertUseCase.addOptionResult(
                    concert.getId(),
                    request.getConcertDate().plusDays(index),
                    venueId
            );
            concertUseCase.createSeats(option.getId(), request.getSeatCount());
            optionIds.add(option.getId());
        }

        return ResponseEntity.ok(
                "Setup completed: ConcertID=" + concert.getId()
                        + ", OptionID=" + optionIds.get(0)
                        + ", OptionCount=" + optionIds.size()
                        + ", OptionIDs=" + optionIds
        );
    }

    /**
     * [Admin/Test] 테스트 데이터 삭제 (Cleanup)
     */
    @DeleteMapping("/cleanup/{concertId}")
    public ResponseEntity<String> cleanupConcert(@PathVariable Long concertId) {
        concertUseCase.deleteConcert(concertId);
        return ResponseEntity.ok("Cleanup completed for ConcertID: " + concertId);
    }

    /**
     * 전체 공연 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ConcertResponse>> getConcerts() {
        return ResponseEntity.ok(concertUseCase.getConcertResults().stream()
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
        String requestedSortField = sortTokens.length > 0 ? sortTokens[0] : "id";
        boolean queueSortBySaleOpen = isQueueSortField(requestedSortField);
        String sortField = resolveSortField(queueSortBySaleOpen ? "id" : requestedSortField);
        Sort.Direction direction = resolveDirection(sortTokens.length > 1 ? sortTokens[1] : "asc");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        var result = concertUseCase.searchConcertResults(keyword, artistName, entertainmentName, pageable)
                .map(ConcertResponse::from);

        if (queueSortBySaleOpen) {
            return ResponseEntity.ok(toQueueSortedPage(keyword, artistName, entertainmentName, page, size, direction, result));
        }

        return ResponseEntity.ok(ConcertSearchPageResponse.from(result));
    }

    private ConcertSearchPageResponse toQueueSortedPage(
            String keyword,
            String artistName,
            String entertainmentName,
            int page,
            int size,
            Sort.Direction direction,
            Page<ConcertResponse> initialPage
    ) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, size);
        long totalElements = initialPage.getTotalElements();

        Page<ConcertResponse> fullPage = initialPage;
        if (normalizedPage != 0 || initialPage.getContent().size() < totalElements) {
            int fetchSize = totalElements > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalElements;
            fetchSize = Math.max(fetchSize, normalizedSize);
            fullPage = concertUseCase.searchConcertResults(
                    keyword,
                    artistName,
                    entertainmentName,
                    PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.ASC, "id"))
            ).map(ConcertResponse::from);
            totalElements = fullPage.getTotalElements();
        }

        List<ConcertResponse> sortedItems = new ArrayList<>(fullPage.getContent());
        sortedItems.sort(queueSortComparator(direction));

        long fromIndexLong = Math.min((long) normalizedPage * normalizedSize, (long) sortedItems.size());
        long toIndexLong = Math.min(fromIndexLong + normalizedSize, (long) sortedItems.size());
        int fromIndex = (int) fromIndexLong;
        int toIndex = (int) toIndexLong;

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        boolean hasNext = normalizedPage + 1 < totalPages;

        return ConcertSearchPageResponse.builder()
                .items(sortedItems.subList(fromIndex, toIndex))
                .page(normalizedPage)
                .size(normalizedSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }

    private Comparator<ConcertResponse> queueSortComparator(Sort.Direction direction) {
        Comparator<ConcertResponse> comparator = Comparator
                .comparing(ConcertResponse::getSaleOpensAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ConcertResponse::getTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ConcertResponse::getId);
        return direction == Sort.Direction.DESC ? comparator.reversed() : comparator;
    }

    /**
     * 오늘 오픈 임박 / 매진 임박 하이라이트 조회
     */
    @GetMapping("/highlights")
    public ResponseEntity<ConcertHighlightsResponse> getHighlights(
            @RequestParam(defaultValue = "3") int openingSoonLimit,
            @RequestParam(defaultValue = "3") int sellOutRiskLimit
    ) {
        return ResponseEntity.ok(ConcertHighlightsResponse.from(
                concertUseCase.getConcertHighlights(openingSoonLimit, sellOutRiskLimit)
        ));
    }

    /**
     * 공연의 날짜별 옵션 조회
     */
    @GetMapping("/{id}/options")
    public ResponseEntity<List<ConcertOptionResponse>> getOptions(@PathVariable Long id) {
        return ResponseEntity.ok(concertUseCase.getConcertOptionResults(id).stream()
                .map(ConcertOptionResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{concertId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long concertId) {
        ConcertUseCase.ConcertThumbnail thumbnail = concertUseCase.getThumbnail(concertId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(thumbnail.contentType()))
                .body(thumbnail.bytes());
    }

    /**
     * 특정 공연 옵션의 예약 가능 좌석 조회
     */
    @GetMapping("/options/{optionId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long optionId) {
        return ResponseEntity.ok(concertUseCase.getAvailableSeatResults(optionId).stream()
                .map(SeatResponse::from)
                .collect(Collectors.toList()));
    }

    /**
     * 특정 공연 옵션의 좌석 상태맵 조회(AVAILABLE/TEMP_RESERVED/RESERVED)
     */
    @GetMapping("/options/{optionId}/seat-map")
    public ResponseEntity<List<SeatResponse>> getSeatMap(
            @PathVariable Long optionId,
            @RequestParam(required = false, name = "status") List<String> statuses
    ) {
        return ResponseEntity.ok(concertUseCase.getSeatMapResults(optionId, statuses).stream()
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
        SalesPolicyUpsertCommand command = new SalesPolicyUpsertCommand(
                request.getPresaleStartAt(),
                request.getPresaleEndAt(),
                request.getPresaleMinimumTier(),
                request.getGeneralSaleStartAt(),
                request.getMaxReservationsPerUser()
        );
        return ResponseEntity.ok(SalesPolicyResponse.from(salesPolicyUseCase.upsert(concertId, command)));
    }

    /**
     * [Read] Step 11 - 공연 판매 정책 조회
     */
    @GetMapping("/{concertId}/sales-policy")
    public ResponseEntity<SalesPolicyResponse> getSalesPolicy(@PathVariable Long concertId) {
        return ResponseEntity.ok(SalesPolicyResponse.from(salesPolicyUseCase.getByConcertId(concertId)));
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

    private boolean isQueueSortField(String candidate) {
        if (candidate == null) {
            return false;
        }
        return "saleOpensAt".equalsIgnoreCase(candidate)
                || "saleOpensInSeconds".equalsIgnoreCase(candidate);
    }

    private Sort.Direction resolveDirection(String candidate) {
        try {
            return Sort.Direction.fromString(candidate);
        } catch (IllegalArgumentException ignored) {
            return Sort.Direction.ASC;
        }
    }

    private Long resolveVenueId(ConcertSetupRequest request) {
        String venueName = normalize(request.getVenueName());
        if (venueName == null) {
            return null;
        }

        String venueCity = normalize(request.getVenueCity());
        String venueCountryCode = normalize(request.getVenueCountryCode());
        String venueAddress = normalize(request.getVenueAddress());
        return venueRepository.findByNameIgnoreCase(venueName)
                .map(existing -> {
                    existing.update(venueName, venueCity, venueCountryCode, venueAddress);
                    return existing.getId();
                })
                .orElseGet(() -> venueRepository.save(
                        new Venue(venueName, venueCity, venueCountryCode, venueAddress)
                ).getId());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
