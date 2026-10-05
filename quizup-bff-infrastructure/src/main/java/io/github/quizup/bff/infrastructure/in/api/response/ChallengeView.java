package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.ChallengeStatus;

import java.time.Instant;

/**
 * Vue d'un défi nominatif (intention asynchrone) : sujet, adversaires, statut et salle créée
 * à l'acceptation (`roomId`).
 */
public record ChallengeView(
        String challengeId,
        TopicRefView topic,
        UserRefView challenger,
        UserRefView opponent,
        ChallengeStatus status,
        String roomId,
        Instant createdAt,
        Instant expiresAt
) {
}
