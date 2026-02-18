package com.ticketrush.domain.agency.service;

import com.ticketrush.domain.agency.Agency;

import java.util.List;

public interface AgencyService {
    Agency create(String name, String countryCode, String homepageUrl);
    List<Agency> getAll();
    Agency getById(Long id);
    Agency update(Long id, String name, String countryCode, String homepageUrl);
    void delete(Long id);
}
