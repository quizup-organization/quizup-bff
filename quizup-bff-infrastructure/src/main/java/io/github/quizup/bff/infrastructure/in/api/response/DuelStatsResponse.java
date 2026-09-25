package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * Statistiques agrégées de duels d'un joueur.
 */
public record DuelStatsResponse(
        int played,
        int wins,
        int losses,
        int winRate,
        int bestScore,
        int bestStreak
) implements Serializable {
}
