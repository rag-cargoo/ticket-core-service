package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.SeatLayoutCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutSectionRequest {
    private String code;
    private Integer capacity;
    private List<SeatLayoutRowRequest> rows;

    public SeatLayoutCommand.Section toCommand() {
        List<SeatLayoutCommand.Row> mappedRows = rows == null
                ? List.of()
                : rows.stream()
                .map(SeatLayoutRowRequest::toCommand)
                .toList();
        return new SeatLayoutCommand.Section(code, capacity, mappedRows);
    }
}
