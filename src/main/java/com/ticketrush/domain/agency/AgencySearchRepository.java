package com.ticketrush.domain.agency;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AgencySearchRepository {
    Page<Agency> searchPaged(String keyword, Pageable pageable);
}
