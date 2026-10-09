package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;

/**
 * Référence minimale d'un sujet (historique de duel, défi) : libellé + visuel + catégorie.
 */
public record TopicRefView(
        String topicId,
        Map<Language, String> names,
        String category,
        String emoji,
        String color,
        String imageUrl
) {
}
