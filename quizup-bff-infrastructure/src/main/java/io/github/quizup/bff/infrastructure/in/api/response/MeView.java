package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

/**
 * Joueur courant (coquille, écrans profil) : profil + progression + compteurs d'abonnements.
 */
public record MeView(
        String userId,
        String email,
        String pseudonym,
        String bio,
        String country,
        String avatarOptions,
        Language language,
        ProgressionView progression,
        DuelStatsView stats,
        long followingCount,
        long followersCount
) {
}
