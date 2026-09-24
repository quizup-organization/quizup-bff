package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * Catalogue d'une catégorie de sujet (code + libellé d'affichage).
 */
public record TopicCategoryResponse(
        String code,
        String label
) implements Serializable {
}
