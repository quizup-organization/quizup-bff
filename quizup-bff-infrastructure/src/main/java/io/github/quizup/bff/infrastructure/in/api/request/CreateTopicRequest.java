package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création d'un sujet (brouillon) par le joueur courant. L'illustration de couverture est une
 * URL externe optionnelle ; l'emoji et la couleur d'accent alimentent la teinte des cartes.
 */
public record CreateTopicRequest(
        @NotBlank @Size(max = TopicRules.MAX_NAME_LENGTH) String name,
        @Size(max = TopicRules.MAX_DESCRIPTION_LENGTH) String description,
        @NotNull TopicCategory category,
        @Size(max = TopicRules.MAX_EMOJI_LENGTH) String emoji,
        @Size(max = TopicRules.MAX_COLOR_LENGTH) String color,
        @Size(max = TopicRules.MAX_IMAGE_URL_LENGTH)
        @Pattern(regexp = "^https?://.+", message = "doit être une URL http(s)") String imageUrl
) {
}
