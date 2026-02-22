package com.ticketrush.domain.agency;

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
public class AgencySearchRepositoryImpl implements AgencySearchRepository {

    private final EntityManager entityManager;

    @Override
    public Page<Agency> searchPaged(String keyword, Pageable pageable) {
        QAgency agency = QAgency.agency;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanBuilder where = new BooleanBuilder();
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    agency.name.containsIgnoreCase(normalizedKeyword)
                            .or(agency.countryCode.containsIgnoreCase(normalizedKeyword))
            );
        }

        List<Agency> content = queryFactory
                .selectFrom(agency)
                .where(where)
                .orderBy(resolveOrderSpecifiers(agency, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(agency.count())
                .from(agency)
                .where(where)
                .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(QAgency agency, Sort sort) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for (Sort.Order order : sort) {
            boolean asc = order.isAscending();
            String property = order.getProperty();
            if ("name".equalsIgnoreCase(property)) {
                specifiers.add(asc ? agency.name.asc() : agency.name.desc());
                continue;
            }
            if ("countryCode".equalsIgnoreCase(property)) {
                specifiers.add(asc ? agency.countryCode.asc() : agency.countryCode.desc());
                continue;
            }
            specifiers.add(asc ? agency.id.asc() : agency.id.desc());
        }

        if (specifiers.isEmpty()) {
            specifiers.add(agency.id.desc());
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
