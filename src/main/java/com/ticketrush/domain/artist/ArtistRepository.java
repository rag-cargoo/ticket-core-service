package com.ticketrush.domain.artist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    Optional<Artist> findByName(String name);
    Optional<Artist> findByNameIgnoreCase(String name);
}
