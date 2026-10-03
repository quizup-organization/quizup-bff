package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.QuestionChoice;
import jakarta.validation.constraints.NotNull;

/** Mise à jour de la bonne réponse, partagée entre les langues. */
public record UpdateQuestionCorrectAnswerRequest(
        @NotNull QuestionChoice correctAnswer
) {
}
