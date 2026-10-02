package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.game.domain.model.BotDifficulty;
import jakarta.validation.constraints.NotBlank;

/**
 * Création d'un duel contre un bot. {@code difficulty} est optionnelle (défaut NORMAL).
 * Les duels entre humains passent désormais par les salons ({@code /api/lobbies}).
 */
public record CreateGameRequest(
        @NotBlank String topicId,
        BotDifficulty difficulty
) {
}
