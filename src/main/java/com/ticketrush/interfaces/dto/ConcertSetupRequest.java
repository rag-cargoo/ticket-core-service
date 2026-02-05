package com.ticketrush.interfaces.dto;

import java.time.LocalDateTime;

/**
 * [Admin/Test] 공연 및 좌석 일괄 생성 요청 DTO
 */
public record ConcertSetupRequest(
        String title,
        String artistName,
        String agencyName,
        LocalDateTime concertDate,
        int seatCount
) {}
