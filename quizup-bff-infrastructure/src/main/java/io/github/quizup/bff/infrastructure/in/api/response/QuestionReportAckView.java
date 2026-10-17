package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.QuestionReportStatus;

import java.time.Instant;

/** Accusé de dépôt d'un signalement (idempotent : {@code created=false} si déjà ouvert). */
public record QuestionReportAckView(
        String reportId,
        QuestionReportStatus status,
        Instant createdAt,
        boolean created
) {
}
