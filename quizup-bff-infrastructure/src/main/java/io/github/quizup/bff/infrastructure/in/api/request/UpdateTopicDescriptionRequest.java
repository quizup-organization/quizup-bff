package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicRules;
import jakarta.validation.constraints.Size;

/** Mise à jour de la description d'un sujet ({@code null} efface). */
public record UpdateTopicDescriptionRequest(
        @Size(max = TopicRules.MAX_DESCRIPTION_LENGTH) String description
) {
}
