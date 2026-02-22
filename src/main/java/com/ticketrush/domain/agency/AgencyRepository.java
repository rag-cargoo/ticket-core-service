package com.ticketrush.domain.agency;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, Long>, AgencySearchRepository {
    Optional<Agency> findByName(String name);
    Optional<Agency> findByNameIgnoreCase(String name);
}
