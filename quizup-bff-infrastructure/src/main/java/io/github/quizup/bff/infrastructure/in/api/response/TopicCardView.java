package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.TopicCategory;

/**
 * Carte de sujet (catalogue, accueil, sujets suivis).
 */
public record TopicCardView(
        String topicId,
        String name,
        String description,
        TopicCategory category,
        String categoryLabel,
        String emoji,
        String color,
        int followersCount,
        int questionsCount,
        boolean followed
) {
}
