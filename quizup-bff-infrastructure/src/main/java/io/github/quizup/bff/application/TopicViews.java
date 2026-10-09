package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.TopicCardView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.QuestionStatus;
import io.github.quizup.theme.domain.model.Topic;

import java.util.Map;

/**
 * Conversions sujet → vues web partagées par les compositions.
 */
final class TopicViews {

    private TopicViews() {
    }

    static TopicCardView toCard(Topic topic, boolean followed) {
        int questionsCount = topic.questionsCounter() == null
                ? 0
                : topic.questionsCounter().getOrDefault(QuestionStatus.APPROVED, 0);
        return new TopicCardView(
                topic.topicId(),
                topic.names(),
                topic.description(),
                topic.category(),
                topic.category() == null ? null : topic.category().label(),
                topic.emoji(),
                topic.color(),
                topic.imageUrl(),
                topic.followersCounter() == null ? 0 : topic.followersCounter(),
                questionsCount,
                followed,
                topic.status());
    }

    static TopicRefView toRef(String topicId, Topic topic) {
        return topic == null
                ? new TopicRefView(topicId, Map.of(Language.FR, topicId), null, null, null, null)
                : new TopicRefView(
                        topic.topicId(),
                        topic.names(),
                        topic.category() == null ? null : topic.category().name(),
                        topic.emoji(),
                        topic.color(),
                        topic.imageUrl());
    }
}
