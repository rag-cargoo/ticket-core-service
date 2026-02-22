package com.ticketrush.domain.entertainment.service;

import com.ticketrush.domain.entertainment.Entertainment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EntertainmentService {
    Entertainment create(String name, String countryCode, String homepageUrl);
    Page<Entertainment> search(String keyword, Pageable pageable);
    List<Entertainment> getAll();
    Entertainment getById(Long id);
    Entertainment update(Long id, String name, String countryCode, String homepageUrl);
    void delete(Long id);
}
