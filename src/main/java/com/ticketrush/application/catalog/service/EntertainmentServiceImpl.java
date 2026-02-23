package com.ticketrush.application.catalog.service;

import com.ticketrush.application.catalog.model.EntertainmentResult;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntertainmentServiceImpl implements EntertainmentService {

    private final EntertainmentRepository entertainmentRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public EntertainmentResult create(String name, String countryCode, String homepageUrl) {
        String normalizedName = normalizeRequired(name, "name");
        entertainmentRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Entertainment already exists: " + existing.getName());
        });
        Entertainment saved = entertainmentRepository.save(new Entertainment(normalizedName, countryCode, homepageUrl));
        return EntertainmentResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntertainmentResult> search(String keyword, Pageable pageable) {
        return entertainmentRepository.searchPaged(normalize(keyword), pageable)
                .map(EntertainmentResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntertainmentResult> getAll() {
        return entertainmentRepository.findAll().stream()
                .map(EntertainmentResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntertainmentResult getById(Long id) {
        return EntertainmentResult.from(requireEntertainment(id));
    }

    @Override
    @Transactional
    public EntertainmentResult update(Long id, String name, String countryCode, String homepageUrl) {
        Entertainment entertainment = requireEntertainment(id);
        String normalizedName = normalizeRequired(name, "name");
        entertainmentRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Entertainment already exists: " + existing.getName());
                });
        entertainment.rename(normalizedName);
        entertainment.updateMetadata(countryCode, homepageUrl);
        return EntertainmentResult.from(entertainment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!entertainmentRepository.existsById(id)) {
            throw new IllegalArgumentException("Entertainment not found: " + id);
        }
        if (artistRepository.existsByEntertainmentId(id)) {
            throw new IllegalArgumentException("Entertainment is referenced by artists: " + id);
        }
        entertainmentRepository.deleteById(id);
    }

    private Entertainment requireEntertainment(Long id) {
        return entertainmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entertainment not found: " + id));
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
