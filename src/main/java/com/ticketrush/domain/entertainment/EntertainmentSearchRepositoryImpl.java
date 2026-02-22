package com.ticketrush.domain.entertainment;

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
public class EntertainmentSearchRepositoryImpl implements EntertainmentSearchRepository {

    private final EntityManager entityManager;

    @Override
    public Page<Entertainment> searchPaged(String keyword, Pageable pageable) {
        QEntertainment entertainment = QEntertainment.entertainment;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanBuilder where = new BooleanBuilder();
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    entertainment.name.containsIgnoreCase(normalizedKeyword)
                            .or(entertainment.countryCode.containsIgnoreCase(normalizedKeyword))
            );
        }

        List<Entertainment> content = queryFactory
                .selectFrom(entertainment)
                .where(where)
                .orderBy(resolveOrderSpecifiers(entertainment, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(entertainment.count())
                .from(entertainment)
                .where(where)
                .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(QEntertainment entertainment, Sort sort) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for (Sort.Order order : sort) {
            boolean asc = order.isAscending();
            String property = order.getProperty();
            if ("name".equalsIgnoreCase(property)) {
                specifiers.add(asc ? entertainment.name.asc() : entertainment.name.desc());
                continue;
            }
            if ("countryCode".equalsIgnoreCase(property)) {
                specifiers.add(asc ? entertainment.countryCode.asc() : entertainment.countryCode.desc());
                continue;
            }
            specifiers.add(asc ? entertainment.id.asc() : entertainment.id.desc());
        }

        if (specifiers.isEmpty()) {
            specifiers.add(entertainment.id.desc());
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
