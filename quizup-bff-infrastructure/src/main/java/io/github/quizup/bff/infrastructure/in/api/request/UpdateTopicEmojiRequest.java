package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.theme.domain.model.TopicRules;
import jakarta.validation.constraints.Size;

/** Mise à jour de l'emoji d'un sujet ({@code null} efface). */
public record UpdateTopicEmojiRequest(
        @Size(max = TopicRules.MAX_EMOJI_LENGTH) String emoji
) {
}
