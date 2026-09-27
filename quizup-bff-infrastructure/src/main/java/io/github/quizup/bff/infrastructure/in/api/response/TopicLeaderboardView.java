package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Classement d'un sujet : page d'entrées + rang du joueur courant dans la même portée.
 */
public record TopicLeaderboardView(
        PageResponse<EntryView> entries,
        EntryView me
) {

    public record EntryView(
            int rank,
            String userId,
            String displayName,
            String avatarOptions,
            String country,
            int level,
            int totalXp,
            int monthlyXp
    ) {
    }
}
