package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.TopicStatus;

import java.util.Map;

/** Option du filtre « Sujet » et du formulaire de création admin. */
public record AdminTopicOptionView(
        String topicId,
        Map<Language, String> names,
        TopicStatus status,
        Integer questionsCount
) {
}
