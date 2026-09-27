package io.github.quizup.bff.infrastructure.in.api.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Activité journalière d'un joueur (série + graphe de contribution).
 */
public record ActivityViewResponse(
        String userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActiveDate,
        int totalActiveDays,
        List<DayView> days
) {

    public record DayView(LocalDate date, int games) {
    }
}
