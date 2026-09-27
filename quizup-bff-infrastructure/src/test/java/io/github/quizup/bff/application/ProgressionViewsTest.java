package io.github.quizup.bff.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionViewsTest {

    @Test
    void level_progress_starts_at_zero_for_level_one() {
        assertEquals(0, ProgressionViews.levelProgressPercent(0, 1));
        assertEquals(50, ProgressionViews.levelProgressPercent(50, 1));
        assertEquals(99, ProgressionViews.levelProgressPercent(99, 1));
    }

    @Test
    void level_progress_uses_quadratic_bounds() {
        assertEquals(0, ProgressionViews.levelProgressPercent(100, 2));
        assertEquals(50, ProgressionViews.levelProgressPercent(250, 2));
        assertEquals(99, ProgressionViews.levelProgressPercent(399, 2));
        assertEquals(0, ProgressionViews.levelProgressPercent(400, 3));
    }

    @Test
    void level_progress_is_capped_at_hundred_when_input_is_inconsistent() {
        assertEquals(100, ProgressionViews.levelProgressPercent(150, 1));
    }
}
