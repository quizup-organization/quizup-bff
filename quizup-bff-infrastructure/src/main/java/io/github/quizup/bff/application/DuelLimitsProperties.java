package io.github.quizup.bff.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limites de création de duels ({@code quizup.duel.limits.*}). Valeurs surchargeables par
 * configuration/GitOps : la politique premium lira ces bornes comme plafond actuel avant de les
 * remplacer par un quota par joueur.
 */
@ConfigurationProperties(prefix = "quizup.duel.limits")
public record DuelLimitsProperties(
        /** Nombre maximal de défis PENDING créés par un joueur (0 = illimité). */
        @DefaultValue("5") int pendingChallenges,
        /** Nombre maximal de salons WAITING_PLAYERS créés par un joueur (0 = illimité). */
        @DefaultValue("5") int openRooms) {
}
