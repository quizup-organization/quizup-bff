package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Référence minimale d'un sujet (historique de duel, défi) : libellé + visuel + catégorie.
 */
public record TopicRefView(
        String topicId,
        String name,
        String category,
        String emoji,
        String color,
        String imageUrl
) {
}
