package com.ticketrush.domain.agency.service;

import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
import com.ticketrush.domain.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyServiceImpl implements AgencyService {

    private final AgencyRepository agencyRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public Agency create(String name, String countryCode, String homepageUrl) {
        String normalizedName = normalizeRequired(name, "name");
        agencyRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Agency already exists: " + existing.getName());
        });
        return agencyRepository.save(new Agency(normalizedName, countryCode, homepageUrl));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Agency> search(String keyword, Pageable pageable) {
        return agencyRepository.searchPaged(normalize(keyword), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agency> getAll() {
        return agencyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Agency getById(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found: " + id));
    }

    @Override
    @Transactional
    public Agency update(Long id, String name, String countryCode, String homepageUrl) {
        Agency agency = getById(id);
        String normalizedName = normalizeRequired(name, "name");
        agencyRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Agency already exists: " + existing.getName());
                });
        agency.rename(normalizedName);
        agency.updateMetadata(countryCode, homepageUrl);
        return agency;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!agencyRepository.existsById(id)) {
            throw new IllegalArgumentException("Agency not found: " + id);
        }
        if (artistRepository.existsByAgencyId(id)) {
            throw new IllegalArgumentException("Agency is referenced by artists: " + id);
        }
        agencyRepository.deleteById(id);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
