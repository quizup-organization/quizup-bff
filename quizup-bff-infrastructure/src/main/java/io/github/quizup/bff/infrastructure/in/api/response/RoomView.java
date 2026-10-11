package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.RoomStatus;

import java.time.Instant;

/**
 * Vue d'une salle temps réel : sujet, statut de préparation, adversaire, présences et compte
 * à rebours. Le lien de partage se compose via {@code /join/{roomId}}.
 */
public record RoomView(
        String roomId,
        TopicRefView topic,
        RoomStatus status,
        boolean vsBot,
        /** Vrai si le viewer est l'initiateur (seul habilité à annuler la salle). */
        boolean canCancel,
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
