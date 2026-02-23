package com.ticketrush.application.catalog.port.inbound;

import com.ticketrush.application.catalog.model.PromoterResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromoterUseCase {

    PromoterResult create(String name, String countryCode, String homepageUrl);

    Page<PromoterResult> search(String keyword, Pageable pageable);

    List<PromoterResult> getAll();

    PromoterResult getById(Long id);

    PromoterResult update(Long id, String name, String countryCode, String homepageUrl);

    void delete(Long id);
}
