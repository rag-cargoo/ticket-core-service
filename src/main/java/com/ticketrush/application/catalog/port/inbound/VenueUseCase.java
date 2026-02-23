package com.ticketrush.application.catalog.port.inbound;

import com.ticketrush.application.catalog.model.VenueResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VenueUseCase {

    VenueResult create(String name, String city, String countryCode, String address);

    Page<VenueResult> search(String keyword, Pageable pageable);

    List<VenueResult> getAll();

    VenueResult getById(Long id);

    VenueResult update(Long id, String name, String city, String countryCode, String address);

    void delete(Long id);
}
