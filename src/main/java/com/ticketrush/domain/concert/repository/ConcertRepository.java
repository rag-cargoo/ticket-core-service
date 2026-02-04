package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Query("SELECT c FROM Concert c JOIN FETCH c.options")
    List<Concert> findAllWithOptions();
}
