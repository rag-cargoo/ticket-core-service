package com.ticketrush.domain.promoter;

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
public class PromoterSearchRepositoryImpl implements PromoterSearchRepository {

    private final EntityManager entityManager;

    @Override
    public Page<Promoter> searchPaged(String keyword, Pageable pageable) {
        QPromoter promoter = QPromoter.promoter;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanBuilder where = new BooleanBuilder();
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    promoter.name.containsIgnoreCase(normalizedKeyword)
                            .or(promoter.countryCode.containsIgnoreCase(normalizedKeyword))
            );
        }

        List<Promoter> content = queryFactory
                .selectFrom(promoter)
                .where(where)
                .orderBy(resolveOrderSpecifiers(promoter, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(promoter.count())
                .from(promoter)
                .where(where)
                .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(QPromoter promoter, Sort sort) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for (Sort.Order order : sort) {
            boolean asc = order.isAscending();
            String property = order.getProperty();
            if ("name".equalsIgnoreCase(property)) {
                specifiers.add(asc ? promoter.name.asc() : promoter.name.desc());
                continue;
            }
            if ("countryCode".equalsIgnoreCase(property)) {
                specifiers.add(asc ? promoter.countryCode.asc() : promoter.countryCode.desc());
                continue;
            }
            specifiers.add(asc ? promoter.id.asc() : promoter.id.desc());
        }

        if (specifiers.isEmpty()) {
            specifiers.add(promoter.id.desc());
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
