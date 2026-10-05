package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;

import java.time.Instant;

/**
 * Partie en attente/en cours du joueur (reprise) : sujet, adversaire (null pour un bot),
 * statut serveur et date de création.
 */
public record CurrentGameView(
        String gameId,
        TopicRefView topic,
        UserRefView opponent,
        GamePlayerType opponentType,
        GameStatus status,
        Instant createdAt
) {
}
