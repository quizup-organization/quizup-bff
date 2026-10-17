package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.QuestionReportRules;
import jakarta.validation.constraints.Size;

/** Restauration d'une question archivée. */
public record RestoreQuestionRequest(
        @Size(max = QuestionReportRules.MAX_NOTE_LENGTH) String note
) {
}
