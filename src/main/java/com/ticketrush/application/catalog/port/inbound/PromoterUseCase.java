package com.ticketrush.application.catalog.port.inbound;

import com.ticketrush.domain.promoter.Promoter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromoterUseCase {

    Promoter create(String name, String countryCode, String homepageUrl);

    Page<Promoter> search(String keyword, Pageable pageable);

    List<Promoter> getAll();

    Promoter getById(Long id);

    Promoter update(Long id, String name, String countryCode, String homepageUrl);

    void delete(Long id);
}
