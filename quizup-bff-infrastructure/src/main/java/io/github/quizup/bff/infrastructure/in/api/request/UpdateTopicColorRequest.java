package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicRules;
import jakarta.validation.constraints.Size;

/** Mise à jour de la couleur d'accent d'un sujet ({@code null} efface). */
public record UpdateTopicColorRequest(
        @Size(max = TopicRules.MAX_COLOR_LENGTH) String color
) {
}
