package com.franquix.franchises.domain.model;

import java.util.List;
import java.util.function.Function;

/** One page of results plus the totals a client needs to navigate. */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return (totalElements + size - 1) / size;
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalElements);
    }
}
