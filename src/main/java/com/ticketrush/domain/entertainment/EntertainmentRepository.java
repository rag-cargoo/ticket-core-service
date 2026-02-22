package com.ticketrush.domain.entertainment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntertainmentRepository extends JpaRepository<Entertainment, Long>, EntertainmentSearchRepository {
    Optional<Entertainment> findByName(String name);
    Optional<Entertainment> findByNameIgnoreCase(String name);
}
