package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.ChallengeStatus;

import java.time.Instant;

/**
 * Vue d'un défi nominatif (intention asynchrone) : sujet, adversaires, statut et salle créée
 * à l'acceptation (`roomId`). {@code gameId} référence la partie terminée d'origine d'une
 * revanche ({@code null} pour un défi normal) — jamais la future partie.
 */
public record ChallengeView(
        String challengeId,
        TopicRefView topic,
        UserRefView challenger,
        UserRefView opponent,
        ChallengeStatus status,
        String roomId,
        String gameId,
        Instant createdAt,
        Instant expiresAt
) {
}
