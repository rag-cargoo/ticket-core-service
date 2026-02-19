package com.ticketrush.domain.concert.repository;

import com.ticketrush.domain.concert.entity.Concert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
    boolean existsByArtistId(Long artistId);

    @Query("SELECT c FROM Concert c JOIN FETCH c.options")
    List<Concert> findAllWithOptions();

    @Query(
            value = """
                    SELECT c
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.agency ag
                    WHERE (:keyword IS NULL
                           OR lower(c.title) LIKE lower(concat('%', cast(:keyword as string), '%'))
                           OR lower(a.name) LIKE lower(concat('%', cast(:keyword as string), '%'))
                           OR lower(ag.name) LIKE lower(concat('%', cast(:keyword as string), '%'))
                           OR cast(c.id as string) = cast(:keyword as string))
                      AND (:artistName IS NULL
                           OR lower(a.name) = lower(cast(:artistName as string)))
                      AND (:agencyName IS NULL
                           OR lower(ag.name) = lower(cast(:agencyName as string)))
                    """,
            countQuery = """
                    SELECT count(c)
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.agency ag
                    WHERE (:keyword IS NULL
                           OR lower(c.title) LIKE lower(concat('%', cast(:keyword as string), '%'))
                           OR lower(a.name) LIKE lower(concat('%', cast(:keyword as string), '%'))
                           OR lower(ag.name) LIKE lower(concat('%', cast(:keyword as string), '%'))
                           OR cast(c.id as string) = cast(:keyword as string))
                      AND (:artistName IS NULL
                           OR lower(a.name) = lower(cast(:artistName as string)))
                      AND (:agencyName IS NULL
                           OR lower(ag.name) = lower(cast(:agencyName as string)))
                    """
    )
    @EntityGraph(attributePaths = {"artist", "artist.agency"})
    Page<Concert> searchPaged(@Param("keyword") String keyword,
                              @Param("artistName") String artistName,
                              @Param("agencyName") String agencyName,
                              Pageable pageable);
}
