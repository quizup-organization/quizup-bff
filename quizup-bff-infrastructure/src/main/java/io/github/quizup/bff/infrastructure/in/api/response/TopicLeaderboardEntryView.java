package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * Entrée de classement d'un sujet avec son rang (aligné sur le contrat client).
 */
public record TopicLeaderboardEntryView(
        int rank,
        String topicId,
        String userId,
        String displayName,
        String country,
        int totalXp,
        int monthlyXp,
        int level
) implements Serializable {
}
