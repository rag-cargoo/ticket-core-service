package com.ticketrush.domain.promoter.service;

import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.promoter.Promoter;
import com.ticketrush.domain.promoter.PromoterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoterServiceImpl implements PromoterService {

    private final PromoterRepository promoterRepository;
    private final ConcertRepository concertRepository;

    @Override
    @Transactional
    public Promoter create(String name, String countryCode, String homepageUrl) {
        String normalizedName = normalizeRequired(name, "name");
        promoterRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Promoter already exists: " + existing.getName());
        });
        return promoterRepository.save(new Promoter(normalizedName, countryCode, homepageUrl));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Promoter> search(String keyword, Pageable pageable) {
        return promoterRepository.searchPaged(normalize(keyword), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promoter> getAll() {
        return promoterRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Promoter getById(Long id) {
        return promoterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoter not found: " + id));
    }

    @Override
    @Transactional
    public Promoter update(Long id, String name, String countryCode, String homepageUrl) {
        Promoter promoter = getById(id);
        String normalizedName = normalizeRequired(name, "name");
        promoterRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Promoter already exists: " + existing.getName());
                });
        promoter.update(normalizedName, countryCode, homepageUrl);
        return promoter;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!promoterRepository.existsById(id)) {
            throw new IllegalArgumentException("Promoter not found: " + id);
        }
        if (concertRepository.existsByPromoterId(id)) {
            throw new IllegalArgumentException("Promoter is referenced by concerts: " + id);
        }
        promoterRepository.deleteById(id);
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
