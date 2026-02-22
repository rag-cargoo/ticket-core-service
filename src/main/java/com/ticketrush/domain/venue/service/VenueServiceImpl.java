package com.ticketrush.domain.venue.service;

import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.venue.Venue;
import com.ticketrush.domain.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final ConcertOptionRepository concertOptionRepository;

    @Override
    @Transactional
    public Venue create(String name, String city, String countryCode, String address) {
        String normalizedName = normalizeRequired(name, "name");
        venueRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Venue already exists: " + existing.getName());
        });
        return venueRepository.save(new Venue(normalizedName, city, countryCode, address));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Venue> search(String keyword, Pageable pageable) {
        return venueRepository.searchPaged(normalize(keyword), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venue> getAll() {
        return venueRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Venue getById(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + id));
    }

    @Override
    @Transactional
    public Venue update(Long id, String name, String city, String countryCode, String address) {
        Venue venue = getById(id);
        String normalizedName = normalizeRequired(name, "name");
        venueRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Venue already exists: " + existing.getName());
                });
        venue.update(normalizedName, city, countryCode, address);
        return venue;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!venueRepository.existsById(id)) {
            throw new IllegalArgumentException("Venue not found: " + id);
        }
        if (concertOptionRepository.existsByVenueId(id)) {
            throw new IllegalArgumentException("Venue is referenced by concert options: " + id);
        }
        venueRepository.deleteById(id);
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
