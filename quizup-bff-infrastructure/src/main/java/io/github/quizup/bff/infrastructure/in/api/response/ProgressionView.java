package io.github.quizup.bff.infrastructure.in.api.response;

import java.util.List;

/**
 * Progression d'un joueur telle que consommée par le web (barre de niveau, badges).
 *
 * <p>{@code badges} liste les badges **acquis**, enrichis de leur famille et de leur palier. Le
 * catalogue complet (paliers verrouillés et progression) viendra avec l'écran de collection.
 * {@code title} est le titre de <b>compte</b> transitoire — les titres affichés sont désormais
 * thématiques (titre porté du profil, cf. {@link TitlesView}).</p>
 */
public record ProgressionView(
        int xpTotal,
        int level,
        String title,
        int xpForNextLevel,
        int levelProgressPercent,
        List<BadgeView> badges
) {

    public record BadgeView(String code, String label, String family, String tier) {
    }
}
