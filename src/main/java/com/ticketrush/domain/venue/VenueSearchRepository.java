package com.ticketrush.domain.venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VenueSearchRepository {
    Page<Venue> searchPaged(String keyword, Pageable pageable);
}
