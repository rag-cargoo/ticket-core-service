package com.ticketrush.domain.concert.controller;

import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.interfaces.dto.ConcertResponse;
import com.ticketrush.interfaces.dto.ConcertOptionResponse;
import com.ticketrush.interfaces.dto.SeatResponse;
import com.ticketrush.interfaces.dto.ConcertSetupRequest;
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

    /**
     * [Admin/Test] 공연 및 좌석 일괄 생성
     */
    @PostMapping("/setup")
    public ResponseEntity<String> setupConcert(@RequestBody ConcertSetupRequest request) {
        var concert = concertService.createConcert(request.title(), request.artistName(), request.agencyName());
        var option = concertService.addOption(concert.getId(), request.concertDate());
        concertService.createSeats(option.getId(), request.seatCount());
        
        return ResponseEntity.ok("Setup completed: ConcertID=" + concert.getId() + ", OptionID=" + option.getId());
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
}