package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Fiche joueur complète (profil public + progression + stats + compteurs + présence + suivi).
 */
public record PlayerProfileView(
        String userId,
        String displayName,
        String bio,
        String country,
        String avatarOptions,
        boolean isMe,
        boolean following,
        PresenceView presence,
        ProgressionView progression,
        DuelStatsView stats,
        long followersCount,
        long followingCount
) {
}
