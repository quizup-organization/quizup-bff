package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de progression globale d'un joueur (XP totale, niveau, titre, badges,
 * détail par thème, statistiques de duels).
 *
 * <p>Reproduit le contrat historique de l'API profile (consommé par les clients web/mobile).</p>
 */
public record ProgressionResponse(
        String userId,
        int xpTotal,
        int level,
        String title,
        int xpForNextLevel,
        List<BadgeResponse> badges,
        List<TopicProgressResponse> topics,
        DuelStatsResponse duelStats
) implements Serializable {
}
