package com.ticketrush.domain.promoter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromoterSearchRepository {
    Page<Promoter> searchPaged(String keyword, Pageable pageable);
}
