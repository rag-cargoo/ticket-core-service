package com.ticketrush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcertSearchPageResponse {
    private List<ConcertResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private String serverNow;
    private String realtimeMode;
    private Long hybridPollIntervalMillis;

    public static ConcertSearchPageResponse from(Page<ConcertResponse> page) {
        return from(page, null);
    }

    public static ConcertSearchPageResponse from(Page<ConcertResponse> page, String serverNow) {
        return from(page, serverNow, null, null);
    }

    public static ConcertSearchPageResponse from(
            Page<ConcertResponse> page,
            String serverNow,
            String realtimeMode,
            Long hybridPollIntervalMillis
    ) {
        return ConcertSearchPageResponse.builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .serverNow(serverNow)
                .realtimeMode(realtimeMode)
                .hybridPollIntervalMillis(hybridPollIntervalMillis)
                .build();
    }
}
