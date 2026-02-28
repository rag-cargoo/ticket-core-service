package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.SeatLayoutCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutRequest {
    private List<SeatLayoutSectionRequest> sections;

    public SeatLayoutCommand toCommand() {
        List<SeatLayoutCommand.Section> mappedSections = sections == null
                ? List.of()
                : sections.stream()
                .map(SeatLayoutSectionRequest::toCommand)
                .toList();
        return new SeatLayoutCommand(mappedSections);
    }
}
