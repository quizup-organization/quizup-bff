package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.RoomStatus;

import java.time.Instant;

/**
 * Vue d'une salle temps réel : sujet, phase, adversaire, présences et compte à rebours.
 * Le lien de partage se compose via {@code /join/{roomId}}.
 */
public record RoomView(
        String roomId,
        TopicRefView topic,
        RoomStatus status,
        RoomPhase phase,
        UserRefView opponent,
        boolean initiatorPresent,
        boolean participantPresent,
        Instant readyDeadlineAt,
        String gameId,
        Instant createdAt,
        Instant expiresAt,
        Instant updatedAt
) {
}
