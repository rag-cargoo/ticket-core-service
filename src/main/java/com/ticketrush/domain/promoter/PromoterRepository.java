package com.ticketrush.domain.promoter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoterRepository extends JpaRepository<Promoter, Long>, PromoterSearchRepository {
    Optional<Promoter> findByNameIgnoreCase(String name);
}
