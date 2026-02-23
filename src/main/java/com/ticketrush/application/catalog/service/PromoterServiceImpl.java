package com.ticketrush.application.catalog.service;

import com.ticketrush.application.catalog.model.PromoterResult;
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
    public PromoterResult create(String name, String countryCode, String homepageUrl) {
        String normalizedName = normalizeRequired(name, "name");
        promoterRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Promoter already exists: " + existing.getName());
        });
        Promoter saved = promoterRepository.save(new Promoter(normalizedName, countryCode, homepageUrl));
        return PromoterResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromoterResult> search(String keyword, Pageable pageable) {
        return promoterRepository.searchPaged(normalize(keyword), pageable)
                .map(PromoterResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromoterResult> getAll() {
        return promoterRepository.findAll().stream()
                .map(PromoterResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromoterResult getById(Long id) {
        return PromoterResult.from(requirePromoter(id));
    }

    @Override
    @Transactional
    public PromoterResult update(Long id, String name, String countryCode, String homepageUrl) {
        Promoter promoter = requirePromoter(id);
        String normalizedName = normalizeRequired(name, "name");
        promoterRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Promoter already exists: " + existing.getName());
                });
        promoter.update(normalizedName, countryCode, homepageUrl);
        return PromoterResult.from(promoter);
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

    private Promoter requirePromoter(Long id) {
        return promoterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoter not found: " + id));
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
