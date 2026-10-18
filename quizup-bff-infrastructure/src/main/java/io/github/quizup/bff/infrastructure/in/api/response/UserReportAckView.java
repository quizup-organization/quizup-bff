package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.identity.domain.model.UserReportStatus;

import java.time.Instant;

public record UserReportAckView(
        String reportId,
        UserReportStatus status,
        Instant createdAt,
        boolean created
) {
}
