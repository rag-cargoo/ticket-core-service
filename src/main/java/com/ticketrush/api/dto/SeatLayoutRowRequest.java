package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.SeatLayoutCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutRowRequest {
    private String label;
    private Integer from;
    private Integer to;

    public SeatLayoutCommand.Row toCommand() {
        return new SeatLayoutCommand.Row(label, from, to);
    }
}
