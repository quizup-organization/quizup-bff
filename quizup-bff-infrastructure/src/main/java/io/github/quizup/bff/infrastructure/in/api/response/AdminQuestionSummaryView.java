package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionDifficulty;
import io.github.quizup.theme.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.Map;

/** Ligne de la file des questions signalées (groupée par question). */
public record AdminQuestionSummaryView(
        String questionId,
        TopicRefView topic,
        Map<Language, String> texts,
        String imageUrl,
        QuestionStatus status,
        QuestionDifficulty difficulty,
        boolean suspended,
        boolean archived,
        int openReportCount,
        int totalReportCount,
        Instant lastReportedAt
) {
}
