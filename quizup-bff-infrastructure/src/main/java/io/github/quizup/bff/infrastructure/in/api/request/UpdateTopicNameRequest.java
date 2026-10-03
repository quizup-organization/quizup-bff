package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mise à jour du nom d'un sujet (propriétaire uniquement). */
public record UpdateTopicNameRequest(
        @NotBlank @Size(max = TopicRules.MAX_NAME_LENGTH) String name
) {
}
