package com.ticketrush.domain.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long>, VenueSearchRepository {
    Optional<Venue> findByNameIgnoreCase(String name);
}
