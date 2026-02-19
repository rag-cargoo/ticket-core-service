package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.ConcertOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertOptionRepository extends JpaRepository<ConcertOption, Long> {
    List<ConcertOption> findByConcertIdOrderByConcertDateAsc(Long concertId);
}
