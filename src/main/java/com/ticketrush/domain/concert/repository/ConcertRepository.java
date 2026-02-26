package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Concert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
    boolean existsByArtistId(Long artistId);
    boolean existsByPromoterId(Long promoterId);
    Optional<Concert> findByTitleIgnoreCase(String title);

    @Query("SELECT c FROM Concert c JOIN FETCH c.options")
    List<Concert> findAllWithOptions();

    @EntityGraph(attributePaths = {"artist", "artist.entertainment"})
    @Query("SELECT c FROM Concert c")
    List<Concert> findAllWithArtistAndEntertainment();

    @Query(
            value = """
                    SELECT c
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.entertainment ag
                    WHERE (:keywordPattern IS NULL
                           OR lower(c.title) LIKE :keywordPattern
                           OR lower(a.name) LIKE :keywordPattern
                           OR lower(ag.name) LIKE :keywordPattern
                           OR (:keywordId IS NOT NULL AND c.id = :keywordId))
                      AND (:artistNameLower IS NULL
                           OR lower(a.name) = :artistNameLower)
                      AND (:entertainmentNameLower IS NULL
                           OR lower(ag.name) = :entertainmentNameLower)
                    """,
            countQuery = """
                    SELECT count(c)
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.entertainment ag
                    WHERE (:keywordPattern IS NULL
                           OR lower(c.title) LIKE :keywordPattern
                           OR lower(a.name) LIKE :keywordPattern
                           OR lower(ag.name) LIKE :keywordPattern
                           OR (:keywordId IS NOT NULL AND c.id = :keywordId))
                      AND (:artistNameLower IS NULL
                           OR lower(a.name) = :artistNameLower)
                      AND (:entertainmentNameLower IS NULL
                           OR lower(ag.name) = :entertainmentNameLower)
                    """
    )
    @EntityGraph(attributePaths = {"artist", "artist.entertainment", "promoter"})
    Page<Concert> searchPaged(@Param("keywordPattern") String keywordPattern,
                              @Param("keywordId") Long keywordId,
                              @Param("artistNameLower") String artistNameLower,
                              @Param("entertainmentNameLower") String entertainmentNameLower,
                              Pageable pageable);
}
