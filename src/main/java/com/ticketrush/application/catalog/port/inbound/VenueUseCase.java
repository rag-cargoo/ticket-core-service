package com.ticketrush.application.catalog.port.inbound;

import com.ticketrush.domain.venue.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VenueUseCase {

    Venue create(String name, String city, String countryCode, String address);

    Page<Venue> search(String keyword, Pageable pageable);

    List<Venue> getAll();

    Venue getById(Long id);

    Venue update(Long id, String name, String city, String countryCode, String address);

    void delete(Long id);
}
