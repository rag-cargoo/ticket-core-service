package com.ticketrush.application.catalog.port.inbound;

import com.ticketrush.application.catalog.model.EntertainmentResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EntertainmentUseCase {

    EntertainmentResult create(String name, String countryCode, String homepageUrl);

    Page<EntertainmentResult> search(String keyword, Pageable pageable);

    List<EntertainmentResult> getAll();

    EntertainmentResult getById(Long id);

    EntertainmentResult update(Long id, String name, String countryCode, String homepageUrl);

    void delete(Long id);
}
