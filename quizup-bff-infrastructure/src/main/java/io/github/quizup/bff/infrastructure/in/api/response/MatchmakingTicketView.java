package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.MatchmakingStatus;

import java.time.Instant;

/**
 * Vue d'une recherche d'appariement public (« Défier le monde »).
 */
public record MatchmakingTicketView(
        String ticketId,
        String topicId,
        MatchmakingStatus status,
        String gameId,
        String opponentId,
        boolean vsBot,
        Instant createdAt,
        Instant updatedAt
) {
}
