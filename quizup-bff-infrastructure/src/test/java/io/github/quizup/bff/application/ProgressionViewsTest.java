package io.github.quizup.bff.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionViewsTest {

    @Test
    void level_progress_starts_at_zero_for_level_one() {
        assertEquals(0, ProgressionViews.levelProgressPercent(0, 1));
        assertEquals(50, ProgressionViews.levelProgressPercent(40, 1));
        assertEquals(98, ProgressionViews.levelProgressPercent(79, 1));
    }

    @Test
    void level_progress_follows_the_accelerated_curve_bounds() {
        // Courbe explicite : le niveau 2 va de 80 à 260 XP, le niveau 3 démarre à 260.
        assertEquals(0, ProgressionViews.levelProgressPercent(80, 2));
        assertEquals(50, ProgressionViews.levelProgressPercent(170, 2));
        assertEquals(99, ProgressionViews.levelProgressPercent(259, 2));
        assertEquals(0, ProgressionViews.levelProgressPercent(260, 3));
    }

    @Test
    void level_progress_is_capped_at_hundred_when_input_is_inconsistent() {
        assertEquals(100, ProgressionViews.levelProgressPercent(150, 1));
    }
}
