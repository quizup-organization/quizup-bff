package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicStatus;

import java.util.Map;

/**
 * Carte de sujet (catalogue, accueil, sujets suivis, mes sujets).
 */
public record TopicCardView(
        String topicId,
        Map<Language, String> names,
        String description,
        TopicCategory category,
        String categoryLabel,
        String emoji,
        String color,
        String imageUrl,
        int followersCount,
        int questionsCount,
        boolean followed,
        TopicStatus status
) {
}
