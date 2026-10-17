package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.QuestionReportResolution;
import io.github.quizup.theme.domain.model.QuestionReportRules;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Clôture d'un ou plusieurs signalements (résolu ou ignoré). */
public record ResolveQuestionReportsRequest(
        @NotNull QuestionReportResolution resolution,
        @Size(max = QuestionReportRules.MAX_NOTE_LENGTH) String note
) {
}
