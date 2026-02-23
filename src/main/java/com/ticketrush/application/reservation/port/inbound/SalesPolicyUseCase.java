package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.SalesPolicyResult;
import com.ticketrush.application.reservation.model.SalesPolicyUpsertCommand;

public interface SalesPolicyUseCase {

    SalesPolicyResult upsert(Long concertId, SalesPolicyUpsertCommand command);

    SalesPolicyResult getByConcertId(Long concertId);
}
