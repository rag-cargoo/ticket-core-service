package com.ticketrush.application.catalog.service;

import com.ticketrush.application.catalog.model.ArtistResult;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final EntertainmentRepository entertainmentRepository;
    private final ConcertRepository concertRepository;

    @Override
    @Transactional
    public ArtistResult create(String name, Long entertainmentId, String displayName, String genre, LocalDate debutDate) {
        String normalizedName = normalizeRequired(name, "name");
        Entertainment entertainment = getEntertainment(entertainmentId);
        artistRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Artist already exists: " + existing.getName());
        });
        Artist saved = artistRepository.save(new Artist(normalizedName, entertainment, displayName, genre, debutDate));
        return ArtistResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArtistResult> search(String keyword, Long entertainmentId, Pageable pageable) {
        return artistRepository.searchPaged(normalize(keyword), entertainmentId, pageable)
                .map(ArtistResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResult> getAll() {
        return artistRepository.findAll().stream()
                .map(ArtistResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistResult getById(Long id) {
        return ArtistResult.from(requireArtist(id));
    }

    @Override
    @Transactional
    public ArtistResult update(Long id, String name, Long entertainmentId, String displayName, String genre, LocalDate debutDate) {
        Artist artist = requireArtist(id);
        String normalizedName = normalizeRequired(name, "name");
        Entertainment entertainment = getEntertainment(entertainmentId);
        artistRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Artist already exists: " + existing.getName());
                });

        artist.rename(normalizedName);
        artist.updateProfile(entertainment, displayName, genre, debutDate);
        return ArtistResult.from(artist);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!artistRepository.existsById(id)) {
            throw new IllegalArgumentException("Artist not found: " + id);
        }
        if (concertRepository.existsByArtistId(id)) {
            throw new IllegalArgumentException("Artist is referenced by concerts: " + id);
        }
        artistRepository.deleteById(id);
    }

    private Artist requireArtist(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + id));
    }

    private Entertainment getEntertainment(Long entertainmentId) {
        if (entertainmentId == null) {
            throw new IllegalArgumentException("entertainmentId is required");
        }
        return entertainmentRepository.findById(entertainmentId)
                .orElseThrow(() -> new IllegalArgumentException("Entertainment not found: " + entertainmentId));
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
