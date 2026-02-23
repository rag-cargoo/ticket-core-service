package com.ticketrush.application.catalog.port.inbound;

import com.ticketrush.application.catalog.model.ArtistResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ArtistUseCase {

    ArtistResult create(String name, Long entertainmentId, String displayName, String genre, LocalDate debutDate);

    Page<ArtistResult> search(String keyword, Long entertainmentId, Pageable pageable);

    List<ArtistResult> getAll();

    ArtistResult getById(Long id);

    ArtistResult update(Long id, String name, Long entertainmentId, String displayName, String genre, LocalDate debutDate);

    void delete(Long id);
}
