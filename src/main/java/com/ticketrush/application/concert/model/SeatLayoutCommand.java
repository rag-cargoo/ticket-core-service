package com.ticketrush.application.concert.model;

import java.util.List;

public record SeatLayoutCommand(List<Section> sections) {

    public static SeatLayoutCommand singleSectionCount(int seatCount) {
        if (seatCount <= 0) {
            throw new IllegalArgumentException("seatCount must be greater than 0");
        }
        return new SeatLayoutCommand(
                List.of(
                        new Section(
                                "A",
                                seatCount,
                                List.of()
                        )
                )
        );
    }

    public record Section(
            String code,
            Integer capacity,
            List<Row> rows
    ) {
    }

    public record Row(
            String label,
            Integer from,
            Integer to
    ) {
    }
}
