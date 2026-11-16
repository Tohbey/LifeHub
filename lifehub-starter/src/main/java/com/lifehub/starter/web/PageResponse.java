package com.lifehub.starter.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** The response shape of every paginated list endpoint. Pages are zero based. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    /** Converts a Spring Data page into the API's page shape, mapping each item. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
