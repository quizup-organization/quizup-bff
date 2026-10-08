package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Écran de résultat d'un duel terminé : score, détail de la performance, récompense XP de la
 * partie ({@code null} si la progression n'est pas encore projetée) et état de progression du
 * joueur (niveau, titre, palier).
 */
public record GameResultView(
        int myScore,
        int opponentScore,
        String winnerId,
        boolean botGame,
        int basePoints,
        int speedBonus,
        int correctAnswers,
        int fastAnswers,
        int answeredRounds,
        int totalRounds,
        RewardView reward,
        ProgressionResultView progression,
        int opponentLevel,
        String opponentTitle
) {

    /** Récompense de la partie : XP totale gagnée et bonus de victoire dérivé du score. */
    public record RewardView(int xp, int victoryBonus) {
    }

    /** Progression courante du joueur (mêmes règles que {@link ProgressionView}). */
    public record ProgressionResultView(
            int xpTotal,
            int level,
            String title,
            int xpForNextLevel,
            int levelProgressPercent
    ) {
    }
}
