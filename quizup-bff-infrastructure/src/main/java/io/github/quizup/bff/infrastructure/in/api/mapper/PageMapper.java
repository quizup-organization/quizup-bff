package io.github.quizup.bff.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;

import java.util.List;

/**
 * Réécrit le contenu d'une {@link SearchResponse} (ex. sous-collection de suivis renvoyant des
 * profils résolus) en conservant la pagination.
 */
public final class PageMapper {

    private PageMapper() {
    }

    public static <T> SearchResponse<T> toResponse(SearchResponse<?> page, List<T> content) {
        return new SearchResponse<>(
                content,
                page.pageNumber(),
                page.pageSize(),
                page.totalElements(),
                page.totalPages(),
                page.sorts(),
                page.first(),
                page.last(),
                page.empty()
        );
    }
}
