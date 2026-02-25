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

    @Query(
            value = """
                    SELECT c
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.entertainment ag
                    WHERE (:hasKeyword = false
                           OR lower(c.title) LIKE :keywordLike
                           OR lower(a.name) LIKE :keywordLike
                           OR lower(ag.name) LIKE :keywordLike
                           OR (:keywordId IS NOT NULL AND c.id = :keywordId))
                      AND (:hasArtistFilter = false
                           OR lower(a.name) = :artistNameLower)
                      AND (:hasEntertainmentFilter = false
                           OR lower(ag.name) = :entertainmentNameLower)
                    """,
            countQuery = """
                    SELECT count(c)
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.entertainment ag
                    WHERE (:hasKeyword = false
                           OR lower(c.title) LIKE :keywordLike
                           OR lower(a.name) LIKE :keywordLike
                           OR lower(ag.name) LIKE :keywordLike
                           OR (:keywordId IS NOT NULL AND c.id = :keywordId))
                      AND (:hasArtistFilter = false
                           OR lower(a.name) = :artistNameLower)
                      AND (:hasEntertainmentFilter = false
                           OR lower(ag.name) = :entertainmentNameLower)
                    """
    )
    @EntityGraph(attributePaths = {"artist", "artist.entertainment", "promoter"})
    Page<Concert> searchPaged(@Param("keywordLike") String keywordLike,
                              @Param("hasKeyword") boolean hasKeyword,
                              @Param("keywordId") Long keywordId,
                              @Param("artistNameLower") String artistNameLower,
                              @Param("hasArtistFilter") boolean hasArtistFilter,
                              @Param("entertainmentNameLower") String entertainmentNameLower,
                              @Param("hasEntertainmentFilter") boolean hasEntertainmentFilter,
                              Pageable pageable);
}
