package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Création d'un défi 1v1.
 */
public record CreateChallengeRequest(
        @NotBlank String challengedId,
        @NotBlank String topicId
) {
}
