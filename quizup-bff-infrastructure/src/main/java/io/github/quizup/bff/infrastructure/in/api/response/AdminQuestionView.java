package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.QuestionSuspensionReason;

import java.time.Instant;
import java.util.List;

/** Question du catalogue admin, avec flags de modération et compteurs de signalements. */
public record AdminQuestionView(
        String questionId,
        TopicRefView topic,
        List<QuestionEditorView.ContentView> contents,
        QuestionChoice correctAnswer,
        String imageUrl,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        boolean suspended,
        QuestionSuspensionReason suspensionReason,
        String suspensionNote,
        Instant suspendedAt,
        String suspendedBy,
        Instant reinstatedAt,
        boolean archived,
        String archiveNote,
        Instant archivedAt,
        String archivedBy,
        Instant restoredAt,
        Instant createdAt,
        Instant updatedAt,
        int openReportCount,
        int totalReportCount,
        Instant lastReportedAt
) {
}
