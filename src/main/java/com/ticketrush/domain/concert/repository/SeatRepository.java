package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    List<Seat> findByConcertOptionIdAndStatus(Long concertOptionId, Seat.SeatStatus status);
    List<Seat> findByConcertOptionIdOrderBySeatNumberAsc(Long concertOptionId);
    List<Seat> findByConcertOptionIdAndStatusInOrderBySeatNumberAsc(Long concertOptionId, Collection<Seat.SeatStatus> statuses);

    @Query("""
            SELECT s.concertOption.concert.id AS concertId,
                   count(s) AS totalSeatCount,
                   sum(CASE WHEN s.status = :availableStatus THEN 1 ELSE 0 END) AS availableSeatCount
            FROM Seat s
            WHERE s.concertOption.concert.id IN :concertIds
            GROUP BY s.concertOption.concert.id
            """)
    List<ConcertSeatStatsProjection> findSeatStatsByConcertIds(
            @Param("concertIds") Collection<Long> concertIds,
            @Param("availableStatus") Seat.SeatStatus availableStatus
    );

    boolean existsByConcertOptionId(Long concertOptionId);
}
