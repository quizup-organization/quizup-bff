package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.DuelStatsView;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionView;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;

import java.util.Comparator;

/**
 * Conversion progression → vues web ; niveau/titre/progression de palier recalculés depuis l'XP
 * (source unique de vérité) plutôt que repris du read model.
 */
public final class ProgressionViews {

    private ProgressionViews() {
    }

    public static ProgressionView toView(PlayerProgress progress) {
        int level = ProgressionRules.levelFor(progress.xpTotal());
        return new ProgressionView(
                progress.xpTotal(),
                level,
                ProgressionRules.titleFor(level),
                ProgressionRules.xpForNextLevel(level),
                levelProgressPercent(progress.xpTotal(), level),
                progress.badges().stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .map(badge -> new ProgressionView.BadgeView(badge.name(), badge.label()))
                        .toList());
    }

    public static DuelStatsView toStats(PlayerProgress progress) {
        int played = progress.gamesPlayed();
        int wins = progress.wins();
        int losses = progress.losses();
        int draws = progress.draws();
        int winPercent = played == 0 ? 0 : (int) Math.round(wins * 100.0 / played);
        return new DuelStatsView(
                played,
                wins,
                losses,
                draws,
                winPercent,
                progress.bestScore(),
                progress.currentWinStreak(),
                progress.bestWinStreak());
    }

    /**
     * Progression dans le palier courant (0–100). Le palier d'un niveau {@code L} va de
     * {@code 100·(L-1)²} à {@code 100·L²} XP.
     */
    public static int levelProgressPercent(int xp, int level) {
        int floor = level <= 1 ? 0 : ProgressionRules.xpForNextLevel(level - 1);
        int next = ProgressionRules.xpForNextLevel(level);
        int span = next - floor;
        if (span <= 0 || xp <= floor) {
            return 0;
        }
        return Math.min(100, (int) ((long) (xp - floor) * 100 / span));
    }
}
