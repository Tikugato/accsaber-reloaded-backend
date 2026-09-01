package com.accsaber.backend.service.snipe;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

import com.accsaber.backend.model.entity.score.SnipeSort;

public record SnipeQuery(Long sniperId, Long targetId, String categoryCode, SnipeSort sort, Sort.Direction direction) {

    public SnipeQuery {
        sort = sort == null ? SnipeSort.GAP : sort;
        direction = direction != null ? direction
                : sort.isDescendingByDefault() ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    public boolean isDefaultOrder() {
        return sort == SnipeSort.GAP && direction.isAscending();
    }

    public String orderSlug() {
        return sort.getSlug() + "-" + direction.name().toLowerCase();
    }

    public String orderLabel() {
        return sort.getLabel() + (direction.isDescending() ? " high to low" : " low to high");
    }

    public Sort toSort() {
        JpaSort primary = JpaSort.unsafe(direction, sort.getExpression());
        Sort ordered = sort.isNullable()
                ? Sort.by(primary.stream().map(order -> order.with(Sort.NullHandling.NULLS_LAST)).toList())
                : primary;
        return ordered.and(JpaSort.unsafe(Sort.Direction.ASC, "s_b.id"));
    }
}
