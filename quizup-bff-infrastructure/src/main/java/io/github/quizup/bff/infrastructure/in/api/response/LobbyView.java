package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.LobbyStatus;

import java.time.Instant;

/**
 * Vue d'une salle temps réel : sujet, phase, adversaire, présences et compte à rebours.
 * Le lien de partage se compose via {@code /join/{lobbyId}}.
 * <p>
 * {@code nominative} = salle issue d'un défi adressé à un joueur précis ; {@code awaitingMe} =
 * le viewer est l'invité et n'a pas encore accepté/refusé (défi encore en attente).
 */
public record LobbyView(
        String lobbyId,
        TopicRefView topic,
        LobbyStatus status,
        LobbyRoomPhase phase,
        UserRefView opponent,
        boolean nominative,
        boolean awaitingMe,
        boolean initiatorPresent,
        boolean participantPresent,
        Instant readyDeadlineAt,
        String missedReason,
        String gameId,
        Instant createdAt,
        Instant expiresAt,
        Instant updatedAt
) {
}
