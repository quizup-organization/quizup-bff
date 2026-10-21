package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Carte joueur (listes Abonnements / Abonnés).
 */
public record PlayerCardView(
        String userId,
        String pseudonym,
        String avatarOptions,
        int level,
        int rating,
        boolean provisional,
        String title,
        boolean following,
        PresenceView presence,
        boolean suspended
) {
}
