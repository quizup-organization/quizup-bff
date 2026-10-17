package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.AdminQuestionSummaryView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminQuestionView;
import io.github.quizup.bff.infrastructure.in.api.response.QuestionEditorView;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionReportCount;
import io.github.quizup.theme.domain.model.QuestionReportSummary;
import io.github.quizup.theme.domain.model.Topic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conversions partagées question → vues admin / auteur. */
final class QuestionViews {

    private QuestionViews() {
    }

    static List<QuestionEditorView.ContentView> contents(Question question) {
        return question.contents().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new QuestionEditorView.ContentView(
                        entry.getKey(),
                        entry.getValue().text(),
                        entry.getValue().answers().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(answer -> new QuestionEditorView.AnswerView(
                                        answer.getKey(), answer.getValue()))
                                .toList()))
                .toList();
    }

    static Map<Language, String> texts(Question question) {
        Map<Language, String> texts = new LinkedHashMap<>();
        question.contents().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> texts.put(entry.getKey(), entry.getValue().text()));
        return texts;
    }

    static AdminQuestionView toAdminView(Question question,
                                         Topic topic,
                                         QuestionReportCount reportCount) {
        return new AdminQuestionView(
                question.questionId(),
                TopicViews.toRef(question.topicId(), topic),
                contents(question),
                question.correctAnswer(),
                question.imageUrl(),
                question.status(),
                question.difficulty(),
                question.suspended(),
                question.suspensionReason(),
                question.suspensionNote(),
                question.suspendedAt(),
                question.suspendedBy(),
                question.reinstatedAt(),
                question.archived(),
                question.archiveNote(),
                question.archivedAt(),
                question.archivedBy(),
                question.restoredAt(),
                question.createdAt(),
                question.updatedAt(),
                reportCount == null ? 0 : reportCount.openCount(),
                reportCount == null ? 0 : reportCount.totalCount(),
                reportCount == null ? null : reportCount.lastReportedAt());
    }

    static AdminQuestionSummaryView toSummaryView(Question question,
                                                  Topic topic,
                                                  QuestionReportSummary summary) {
        return new AdminQuestionSummaryView(
                question.questionId(),
                TopicViews.toRef(question.topicId(), topic),
                texts(question),
                question.imageUrl(),
                question.status(),
                question.difficulty(),
                question.suspended(),
                question.archived(),
                summary == null ? 0 : summary.openCount(),
                summary == null ? 0 : summary.totalCount(),
                summary == null ? null : summary.lastReportedAt());
    }
}
