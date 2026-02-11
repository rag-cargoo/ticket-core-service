package com.ticketrush.domain.reservation.repository;

import com.ticketrush.domain.reservation.entity.SalesPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesPolicyRepository extends JpaRepository<SalesPolicy, Long> {
    Optional<SalesPolicy> findByConcertId(Long concertId);
}
