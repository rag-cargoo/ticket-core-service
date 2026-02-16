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

    @Query("SELECT c FROM Concert c JOIN FETCH c.options")
    List<Concert> findAllWithOptions();

    @Query(
            value = """
                    SELECT c
                    FROM Concert c
                    JOIN c.artist a
                    LEFT JOIN a.agency ag
                    WHERE (:keyword IS NULL
                           OR lower(c.title) LIKE lower(concat('%', :keyword, '%'))
                           OR lower(a.name) LIKE lower(concat('%', :keyword, '%'))
                           OR str(c.id) = :keyword)
                      AND (:artistName IS NULL
                           OR lower(a.name) = lower(:artistName))
                    """,
            countQuery = """
                    SELECT count(c)
                    FROM Concert c
                    JOIN c.artist a
                    WHERE (:keyword IS NULL
                           OR lower(c.title) LIKE lower(concat('%', :keyword, '%'))
                           OR lower(a.name) LIKE lower(concat('%', :keyword, '%'))
                           OR str(c.id) = :keyword)
                      AND (:artistName IS NULL
                           OR lower(a.name) = lower(:artistName))
                    """
    )
    @EntityGraph(attributePaths = {"artist"})
    Page<Concert> searchPaged(@Param("keyword") String keyword,
                              @Param("artistName") String artistName,
                              Pageable pageable);
}
