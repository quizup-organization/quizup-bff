package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.social.domain.model.ChallengeStatus;

import java.time.Instant;
import java.util.List;

/**
 * Carte de défi (liste reçus / envoyés), enrichie adversaire + sujet + vainqueur + actions.
 */
public record ChallengeCardView(
        String challengeId,
        ChallengeDirection direction,
        ChallengeStatus status,
        TopicRefView topic,
        UserRefView opponent,
        Instant createdAt,
        Instant expiresAt,
        String gameId,
        String winnerId,
        List<ChallengeAction> actions
) {
}
