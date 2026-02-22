package com.ticketrush.domain.seed;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedMarkerRepository extends JpaRepository<SeedMarker, Long> {
    boolean existsByMarkerKey(String markerKey);
}
