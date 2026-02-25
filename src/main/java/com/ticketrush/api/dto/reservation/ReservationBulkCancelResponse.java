package com.ticketrush.api.dto.reservation;

import com.ticketrush.application.reservation.model.ReservationLifecycleResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationBulkCancelResponse {
    private List<ReservationLifecycleResponse> cancelled;

    public static ReservationBulkCancelResponse from(List<ReservationLifecycleResult> results) {
        return new ReservationBulkCancelResponse(
                results.stream()
                        .map(ReservationLifecycleResponse::from)
                        .toList()
        );
    }
}
