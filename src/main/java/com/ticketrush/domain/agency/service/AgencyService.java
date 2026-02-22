package com.ticketrush.domain.agency.service;

import com.ticketrush.domain.agency.Agency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AgencyService {
    Agency create(String name, String countryCode, String homepageUrl);
    Page<Agency> search(String keyword, Pageable pageable);
    List<Agency> getAll();
    Agency getById(Long id);
    Agency update(Long id, String name, String countryCode, String homepageUrl);
    void delete(Long id);
}
