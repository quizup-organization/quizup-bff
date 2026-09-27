package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrée en file d'attente de matchmaking.
 */
public record EnqueueMatchmakingRequest(
        @NotBlank String topicId
) {
}
