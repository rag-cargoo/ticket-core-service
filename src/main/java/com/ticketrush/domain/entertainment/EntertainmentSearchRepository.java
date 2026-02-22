package com.ticketrush.domain.entertainment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EntertainmentSearchRepository {
    Page<Entertainment> searchPaged(String keyword, Pageable pageable);
}
