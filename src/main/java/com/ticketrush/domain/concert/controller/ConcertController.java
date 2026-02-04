package com.ticketrush.domain.concert.controller;

import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.interfaces.dto.ConcertOptionResponse;
import com.ticketrush.interfaces.dto.ConcertResponse;
import com.ticketrush.interfaces.dto.SeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    public ResponseEntity<List<ConcertResponse>> getConcerts() {
        var concerts = concertService.getConcerts().stream()
                .map(ConcertResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(concerts);
    }

    @GetMapping("/{concertId}/dates")
    public ResponseEntity<List<ConcertOptionResponse>> getConcertOptions(@PathVariable Long concertId) {
        var options = concertService.getConcertOptions(concertId).stream()
                .map(ConcertOptionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(options);
    }

    @GetMapping("/{concertId}/dates/{optionId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(
            @PathVariable Long concertId,
            @PathVariable Long optionId) {
        var seats = concertService.getAvailableSeats(optionId).stream()
                .map(SeatResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(seats);
    }
}
