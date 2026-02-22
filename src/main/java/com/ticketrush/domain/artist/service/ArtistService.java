package com.ticketrush.domain.artist.service;

import com.ticketrush.domain.artist.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ArtistService {
    Artist create(String name, Long agencyId, String displayName, String genre, LocalDate debutDate);
    Page<Artist> search(String keyword, Long agencyId, Pageable pageable);
    List<Artist> getAll();
    Artist getById(Long id);
    Artist update(Long id, String name, Long agencyId, String displayName, String genre, LocalDate debutDate);
    void delete(Long id);
}
