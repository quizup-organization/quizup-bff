package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;

/**
 * Suggestion de la palette ⌘K : sujet ou joueur.
 * {@code names} est renseigné pour les sujets (résolution par langue côté client, repli {@code label}).
 */
public record SuggestionView(
        Type type,
        String id,
        String label,
        String subtitle,
        String emoji,
        String color,
        String imageUrl,
        String avatarOptions,
        Map<Language, String> names
) {

    public enum Type {
        TOPIC,
        PLAYER
    }
}
