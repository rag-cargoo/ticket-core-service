package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    interface ConcertSeatCountProjection {
        Long getConcertId();
        Long getSeatCount();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    java.util.List<Seat> findByConcertOptionIdAndStatus(Long concertOptionId, Seat.SeatStatus status);

    @Query("""
            SELECT s.concertOption.concert.id AS concertId, count(s) AS seatCount
            FROM Seat s
            WHERE s.concertOption.concert.id IN :concertIds
            GROUP BY s.concertOption.concert.id
            """)
    java.util.List<ConcertSeatCountProjection> countSeatTotalsByConcertIds(@Param("concertIds") Collection<Long> concertIds);

    @Query("""
            SELECT s.concertOption.concert.id AS concertId, count(s) AS seatCount
            FROM Seat s
            WHERE s.concertOption.concert.id IN :concertIds
              AND s.status = :status
            GROUP BY s.concertOption.concert.id
            """)
    java.util.List<ConcertSeatCountProjection> countSeatTotalsByConcertIdsAndStatus(@Param("concertIds") Collection<Long> concertIds,
                                                                                     @Param("status") Seat.SeatStatus status);
}
