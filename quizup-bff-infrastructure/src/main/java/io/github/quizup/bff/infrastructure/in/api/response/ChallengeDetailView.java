package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.social.domain.model.ChallengeStatus;

import java.time.Instant;
import java.util.List;

/**
 * Détail d'un défi : participants, sujet, parties (sync/async), scores, vainqueur et actions.
 */
public record ChallengeDetailView(
        String challengeId,
        ChallengeDirection direction,
        ChallengeStatus status,
        TopicRefView topic,
        UserRefView challenger,
        UserRefView challenged,
        Instant createdAt,
        Instant expiresAt,
        String gameId,
        String replayGameId,
        String myRunGameId,
        String opponentRunGameId,
        Integer challengerScore,
        Integer challengedScore,
        String winnerId,
        Instant completedAt,
        List<ChallengeAction> actions
) {
}
