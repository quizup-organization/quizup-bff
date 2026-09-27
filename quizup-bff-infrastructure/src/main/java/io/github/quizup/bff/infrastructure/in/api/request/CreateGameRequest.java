package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.game.domain.model.BotDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Création d'un duel. {@code BOT} exige {@code difficulty} (défaut NORMAL) ;
 * {@code ASYNC} accepte {@code opponentId} + {@code ghostGameId} pour rejouer le run d'un joueur.
 */
public record CreateGameRequest(
        @NotBlank String topicId,
        @NotNull Mode mode,
        BotDifficulty difficulty,
        String opponentId,
        String ghostGameId
) {

    public enum Mode {
        BOT,
        ASYNC
    }
}
