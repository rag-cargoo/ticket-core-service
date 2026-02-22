package com.ticketrush.domain.artist.service;

import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
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
    private final AgencyRepository agencyRepository;
    private final ConcertRepository concertRepository;

    @Override
    @Transactional
    public Artist create(String name, Long agencyId, String displayName, String genre, LocalDate debutDate) {
        String normalizedName = normalizeRequired(name, "name");
        Agency agency = getAgency(agencyId);
        artistRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new IllegalArgumentException("Artist already exists: " + existing.getName());
        });
        return artistRepository.save(new Artist(normalizedName, agency, displayName, genre, debutDate));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Artist> search(String keyword, Long agencyId, Pageable pageable) {
        return artistRepository.searchPaged(normalize(keyword), agencyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getAll() {
        return artistRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Artist getById(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + id));
    }

    @Override
    @Transactional
    public Artist update(Long id, String name, Long agencyId, String displayName, String genre, LocalDate debutDate) {
        Artist artist = getById(id);
        String normalizedName = normalizeRequired(name, "name");
        Agency agency = getAgency(agencyId);
        artistRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Artist already exists: " + existing.getName());
                });

        artist.rename(normalizedName);
        artist.updateProfile(agency, displayName, genre, debutDate);
        return artist;
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

    private Agency getAgency(Long agencyId) {
        if (agencyId == null) {
            throw new IllegalArgumentException("agencyId is required");
        }
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found: " + agencyId));
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
