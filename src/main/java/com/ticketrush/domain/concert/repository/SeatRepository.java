package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT s.id FROM Seat s WHERE s.concertOption.id = :concertOptionId ORDER BY s.id ASC")
    List<Long> findSeatIdsByConcertOptionId(@Param("concertOptionId") Long concertOptionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = :status WHERE s.concertOption.id = :concertOptionId")
    int updateStatusByConcertOptionId(
            @Param("concertOptionId") Long concertOptionId,
            @Param("status") Seat.SeatStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = :status WHERE s.id IN :seatIds")
    int updateStatusBySeatIds(
            @Param("seatIds") Collection<Long> seatIds,
            @Param("status") Seat.SeatStatus status
    );

    List<Seat> findByConcertOptionIdAndStatus(Long concertOptionId, Seat.SeatStatus status);
    List<Seat> findByConcertOptionId(Long concertOptionId);
    List<Seat> findByConcertOptionIdAndStatusIn(Long concertOptionId, Collection<Seat.SeatStatus> statuses);
    boolean existsByConcertOptionId(Long concertOptionId);

    @Query("""
            SELECT
                co.concert.id AS concertId,
                COUNT(s) AS totalSeatCount,
                SUM(CASE WHEN s.status = :availableStatus THEN 1 ELSE 0 END) AS availableSeatCount
            FROM Seat s
            JOIN s.concertOption co
            WHERE co.concert.id IN :concertIds
            GROUP BY co.concert.id
            """)
    List<ConcertSeatSummaryProjection> summarizeSeatCountsByConcertIds(
            @Param("concertIds") Collection<Long> concertIds,
            @Param("availableStatus") Seat.SeatStatus availableStatus
    );
}
