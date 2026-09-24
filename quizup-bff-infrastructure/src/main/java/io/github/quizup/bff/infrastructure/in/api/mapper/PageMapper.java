package io.github.quizup.bff.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SortResponse;

import java.util.List;

/**
 * Convertit un {@link PageResult} (retourné par le bus de requêtes) en {@link PageResponse}
 * (représentation de transport REST).
 */
public final class PageMapper {

    private PageMapper() {
    }

    public static <T> PageResponse<T> toResponse(PageResult<T> page) {
        return toResponse(page, page.content());
    }

    /**
     * Variante avec contenu réécrit (ex. sous-collection de suivis renvoyant des profils
     * résolus) : les métadonnées de pagination de {@code page} sont conservées.
     */
    public static <T> PageResponse<T> toResponse(PageResult<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.pageNumber(),
                page.pageSize(),
                page.totalElements(),
                page.totalPages(),
                mapSorts(page.sorts()),
                page.first(),
                page.last(),
                page.empty()
        );
    }

    private static List<SortResponse> mapSorts(List<SortCriteria> sorts) {
        return sorts.stream()
                .map(sort -> new SortResponse(sort.property(), sort.direction()))
                .toList();
    }
}
