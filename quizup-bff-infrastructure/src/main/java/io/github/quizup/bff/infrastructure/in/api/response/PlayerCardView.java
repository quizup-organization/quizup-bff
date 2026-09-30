package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Carte joueur (listes Abonnements / Abonnés).
 */
public record PlayerCardView(
        String userId,
        String pseudonym,
        String avatarOptions,
        int level,
        String title,
        boolean following,
        PresenceView presence
) {
}
