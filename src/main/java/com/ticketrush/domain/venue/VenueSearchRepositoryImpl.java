package com.ticketrush.domain.venue;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class VenueSearchRepositoryImpl implements VenueSearchRepository {

    private final EntityManager entityManager;

    @Override
    public Page<Venue> searchPaged(String keyword, Pageable pageable) {
        QVenue venue = QVenue.venue;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanBuilder where = new BooleanBuilder();
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    venue.name.containsIgnoreCase(normalizedKeyword)
                            .or(venue.city.containsIgnoreCase(normalizedKeyword))
                            .or(venue.countryCode.containsIgnoreCase(normalizedKeyword))
            );
        }

        List<Venue> content = queryFactory
                .selectFrom(venue)
                .where(where)
                .orderBy(resolveOrderSpecifiers(venue, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(venue.count())
                .from(venue)
                .where(where)
                .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(QVenue venue, Sort sort) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for (Sort.Order order : sort) {
            boolean asc = order.isAscending();
            String property = order.getProperty();
            if ("name".equalsIgnoreCase(property)) {
                specifiers.add(asc ? venue.name.asc() : venue.name.desc());
                continue;
            }
            if ("city".equalsIgnoreCase(property)) {
                specifiers.add(asc ? venue.city.asc() : venue.city.desc());
                continue;
            }
            if ("countryCode".equalsIgnoreCase(property)) {
                specifiers.add(asc ? venue.countryCode.asc() : venue.countryCode.desc());
                continue;
            }
            specifiers.add(asc ? venue.id.asc() : venue.id.desc());
        }

        if (specifiers.isEmpty()) {
            specifiers.add(venue.id.desc());
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
