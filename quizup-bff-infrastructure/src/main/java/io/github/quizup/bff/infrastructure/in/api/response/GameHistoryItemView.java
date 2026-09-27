package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.game.domain.model.GameMode;

import java.time.Instant;

/**
 * Ligne d'historique d'un duel, relative au joueur consulté.
 */
public record GameHistoryItemView(
        String gameId,
        TopicRefView topic,
        UserRefView opponent,
        String opponentType,
        GameMode mode,
        Outcome outcome,
        int myScore,
        int opponentScore,
        Integer xp,
        Instant playedAt
) {

    public enum Outcome {
        WIN,
        LOSS,
        DRAW,
        PENDING
    }
}
