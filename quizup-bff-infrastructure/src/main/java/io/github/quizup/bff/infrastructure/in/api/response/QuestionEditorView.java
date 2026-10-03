package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.List;

/**
 * Question vue par son auteur : contenus localisés (texte + réponses), bonne réponse, statut de
 * modération et difficulté déduite.
 */
public record QuestionEditorView(
        String questionId,
        List<ContentView> contents,
        QuestionChoice correctAnswer,
        String imageUrl,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        Instant createdAt,
        Instant updatedAt
) {

    public record ContentView(
            Language language,
            String text,
            List<AnswerView> answers
    ) {
    }

    public record AnswerView(
            QuestionChoice choice,
            String text
    ) {
    }
}
