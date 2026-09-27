package io.github.quizup.bff.infrastructure.in.api.response;

import java.util.List;

/**
 * Progression d'un joueur telle que consommée par le web (barre de niveau, badges).
 */
public record ProgressionView(
        int xpTotal,
        int level,
        String title,
        int xpForNextLevel,
        int levelProgressPercent,
        List<BadgeView> badges
) {

    public record BadgeView(String code, String label) {
    }
}
