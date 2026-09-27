package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Catégorie de sujet exposée au web (nom + libellé FR).
 */
public record TopicCategoryView(
        String category,
        String label
) {
}
