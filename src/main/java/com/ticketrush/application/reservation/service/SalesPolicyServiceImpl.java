package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.SalesPolicyResult;
import com.ticketrush.application.reservation.model.SalesPolicyUpsertCommand;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

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
    public SalesPolicyResult upsert(Long concertId, SalesPolicyUpsertCommand command) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found: " + concertId));

        SalesPolicy policy = salesPolicyRepository.findByConcertId(concertId).orElse(null);
        if (policy == null) {
            policy = SalesPolicy.create(
                    concert,
                    command.getPresaleStartAt(),
                    command.getPresaleEndAt(),
                    resolvePresaleMinimumTier(command.getPresaleMinimumTier()),
                    command.getGeneralSaleStartAt(),
                    command.getMaxReservationsPerUser()
            );
        } else {
            policy.update(
                    command.getPresaleStartAt(),
                    command.getPresaleEndAt(),
                    resolvePresaleMinimumTier(command.getPresaleMinimumTier()),
                    command.getGeneralSaleStartAt(),
                    command.getMaxReservationsPerUser()
            );
        }

        return SalesPolicyResult.from(salesPolicyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public SalesPolicyResult getByConcertId(Long concertId) {
        SalesPolicy policy = salesPolicyRepository.findByConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Sales policy not found for concert: " + concertId));
        return SalesPolicyResult.from(policy);
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

    private UserTier resolvePresaleMinimumTier(String rawTier) {
        if (rawTier == null || rawTier.isBlank()) {
            return null;
        }
        try {
            return UserTier.valueOf(rawTier.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid presale minimum tier: " + rawTier);
        }
    }
}
