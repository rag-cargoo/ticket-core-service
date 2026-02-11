package com.ticketrush.api.controller;

import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.api.dto.ConcertResponse;
import com.ticketrush.api.dto.ConcertOptionResponse;
import com.ticketrush.api.dto.SeatResponse;
import com.ticketrush.api.dto.ConcertSetupRequest;
import com.ticketrush.api.dto.reservation.SalesPolicyResponse;
import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.reservation.service.SalesPolicyService;
import lombok.RequiredArgsConstructor;
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
        var concert = concertService.createConcert(request.getTitle(), request.getArtistName(), request.getAgencyName());
        var option = concertService.addOption(concert.getId(), request.getConcertDate());
        concertService.createSeats(option.getId(), request.getSeatCount());
        
        return ResponseEntity.ok("Setup completed: ConcertID=" + concert.getId() + ", OptionID=" + option.getId());
    }

    /**
     * [Admin/Test] 테스트 데이터 삭제 (Cleanup)
     */
    @DeleteMapping("/cleanup/{concertId}")
    public ResponseEntity<String> cleanupConcert(@PathVariable Long concertId) {
        // 실제 운영 환경에서는 사용 금지, 테스트용 로직
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
     * 공연의 날짜별 옵션 조회
     */
    @GetMapping("/{id}/options")
    public ResponseEntity<List<ConcertOptionResponse>> getOptions(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getConcertOptions(id).stream()
                .map(ConcertOptionResponse::from)
                .collect(Collectors.toList()));
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
}
