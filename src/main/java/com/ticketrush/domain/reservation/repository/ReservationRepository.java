package com.ticketrush.domain.reservation.repository;

import com.ticketrush.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);
    List<Reservation> findBySeatId(Long seatId);
    List<Reservation> findByStatusInAndHoldExpiresAtBefore(
            Collection<Reservation.ReservationStatus> statuses,
            LocalDateTime now
    );
    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Reservation r set r.holdExpiresAt = :expiresAt where r.id = :reservationId")
    int updateHoldExpiresAt(@Param("reservationId") Long reservationId, @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reservation r where r.seat.concertOption.concert.id in :concertIds")
    int deleteByConcertIds(@Param("concertIds") Collection<Long> concertIds);

    @Query("""
            select count(r)
            from Reservation r
            where r.user.id = :userId
              and r.seat.concertOption.concert.id = :concertId
              and r.status in :statuses
            """)
    long countByUserIdAndConcertIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("concertId") Long concertId,
            @Param("statuses") Collection<Reservation.ReservationStatus> statuses
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.seat s
            join fetch s.concertOption o
            join fetch o.concert c
            where r.user.id = :userId
              and (:concertId is null or c.id = :concertId)
              and (:optionId is null or o.id = :optionId)
              and (:statusesEmpty = true or r.status in :statuses)
            order by r.id desc
            """)
    List<Reservation> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("concertId") Long concertId,
            @Param("optionId") Long optionId,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("statuses") Collection<Reservation.ReservationStatus> statuses
    );
}
