package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Suggestion de la palette ⌘K : sujet ou joueur.
 */
public record SuggestionView(
        Type type,
        String id,
        String label,
        String subtitle,
        String emoji,
        String color,
        String avatarOptions
) {

    public enum Type {
        TOPIC,
        PLAYER
    }
}
