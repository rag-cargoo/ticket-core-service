package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesPolicyServiceImpl implements SalesPolicyService {

    private static final List<Reservation.ReservationStatus> LIMIT_COUNT_STATUSES = List.of(
            Reservation.ReservationStatus.HOLD,
            Reservation.ReservationStatus.PAYING,
            Reservation.ReservationStatus.CONFIRMED
    );

    private final SalesPolicyRepository salesPolicyRepository;
    private final ReservationRepository reservationRepository;
    private final ConcertRepository concertRepository;

    @Transactional
    public SalesPolicy upsert(Long concertId, SalesPolicyUpsertRequest request) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found: " + concertId));

        SalesPolicy policy = salesPolicyRepository.findByConcertId(concertId).orElse(null);
        if (policy == null) {
            policy = SalesPolicy.create(
                    concert,
                    request.getPresaleStartAt(),
                    request.getPresaleEndAt(),
                    request.getPresaleMinimumTier(),
                    request.getGeneralSaleStartAt(),
                    request.getMaxReservationsPerUser()
            );
        } else {
            policy.update(
                    request.getPresaleStartAt(),
                    request.getPresaleEndAt(),
                    request.getPresaleMinimumTier(),
                    request.getGeneralSaleStartAt(),
                    request.getMaxReservationsPerUser()
            );
        }

        return salesPolicyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public SalesPolicy getByConcertId(Long concertId) {
        return salesPolicyRepository.findByConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Sales policy not found for concert: " + concertId));
    }

    @Transactional(readOnly = true)
    public void validateHoldRequest(User user, Seat seat, LocalDateTime now) {
        Long concertId = seat.getConcertOption().getConcert().getId();
        SalesPolicy policy = salesPolicyRepository.findByConcertId(concertId).orElse(null);
        if (policy == null) {
            return;
        }

        policy.validateHoldRequest(user.getTier(), now);

        long activeReservationCount = reservationRepository.countByUserIdAndConcertIdAndStatusIn(
                user.getId(),
                concertId,
                LIMIT_COUNT_STATUSES
        );
        if (activeReservationCount >= policy.getMaxReservationsPerUser()) {
            throw new IllegalStateException(
                    "Per-user reservation limit exceeded. userId=" + user.getId()
                            + ", concertId=" + concertId
                            + ", limit=" + policy.getMaxReservationsPerUser()
            );
        }
    }
}
