package com.ticketrush.domain.artist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArtistSearchRepository {
    Page<Artist> searchPaged(String keyword, Long agencyId, Pageable pageable);
}
