package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    java.util.List<Seat> findByConcertOptionIdAndStatus(Long concertOptionId, Seat.SeatStatus status);
}
