package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.game.domain.model.GameQuestionChoice;
import jakarta.validation.constraints.NotNull;

/**
 * Réponse à la question courante de l'arène.
 */
public record AnswerQuestionRequest(
        @NotNull GameQuestionChoice choice
) {
}
