package com.ticketrush.domain.artist;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ticketrush.domain.entertainment.QEntertainment;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ArtistSearchRepositoryImpl implements ArtistSearchRepository {

    private final EntityManager entityManager;

    @Override
    public Page<Artist> searchPaged(String keyword, Long entertainmentId, Pageable pageable) {
        QArtist artist = QArtist.artist;
        QEntertainment entertainment = QEntertainment.entertainment;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanBuilder where = new BooleanBuilder();
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    artist.name.containsIgnoreCase(normalizedKeyword)
                            .or(artist.displayName.containsIgnoreCase(normalizedKeyword))
                            .or(artist.genre.containsIgnoreCase(normalizedKeyword))
                            .or(entertainment.name.containsIgnoreCase(normalizedKeyword))
            );
        }
        if (entertainmentId != null) {
            where.and(artist.entertainment.id.eq(entertainmentId));
        }

        List<Artist> content = queryFactory
                .selectFrom(artist)
                .leftJoin(artist.entertainment, entertainment).fetchJoin()
                .where(where)
                .orderBy(resolveOrderSpecifiers(artist, entertainment, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(artist.count())
                .from(artist)
                .leftJoin(artist.entertainment, entertainment)
                .where(where)
                .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(QArtist artist, QEntertainment entertainment, Sort sort) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for (Sort.Order order : sort) {
            boolean asc = order.isAscending();
            String property = order.getProperty();
            if ("name".equalsIgnoreCase(property)) {
                specifiers.add(asc ? artist.name.asc() : artist.name.desc());
                continue;
            }
            if ("displayName".equalsIgnoreCase(property)) {
                specifiers.add(asc ? artist.displayName.asc() : artist.displayName.desc());
                continue;
            }
            if ("genre".equalsIgnoreCase(property)) {
                specifiers.add(asc ? artist.genre.asc() : artist.genre.desc());
                continue;
            }
            if ("debutDate".equalsIgnoreCase(property)) {
                specifiers.add(asc ? artist.debutDate.asc() : artist.debutDate.desc());
                continue;
            }
            if ("entertainmentName".equalsIgnoreCase(property)) {
                specifiers.add(asc ? entertainment.name.asc() : entertainment.name.desc());
                continue;
            }
            specifiers.add(asc ? artist.id.asc() : artist.id.desc());
        }

        if (specifiers.isEmpty()) {
            specifiers.add(artist.id.desc());
        }
        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
