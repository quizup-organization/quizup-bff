package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.matchmaking.domain.model.LobbyStatus;

import java.time.Instant;

/**
 * Vue d'un salon privé : sujet, statut, adversaire (second participant s'il est présent)
 * et partie créée. Le lien de partage se compose via {@code /join/{lobbyId}}.
 * <p>
 * {@code nominative} = défi adressé à un joueur précis ; {@code awaitingMe} = le viewer est
 * l'invité et n'a pas encore accepté/refusé.
 */
public record LobbyView(
        String lobbyId,
        TopicRefView topic,
        LobbyStatus status,
        UserRefView opponent,
        boolean nominative,
        boolean awaitingMe,
        String gameId,
        Instant createdAt,
        Instant expiresAt,
        Instant updatedAt
) {
}
