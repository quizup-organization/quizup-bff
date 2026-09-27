package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Joueur courant (coquille, badge de défis, écrans profil) : profil + progression + compteurs.
 */
public record MeView(
        String userId,
        String email,
        String displayName,
        String bio,
        String country,
        String avatarOptions,
        ProgressionView progression,
        DuelStatsView stats,
        long followingCount,
        long followersCount,
        long pendingChallengesCount
) {
}
