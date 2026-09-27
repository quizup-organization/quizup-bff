package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Enregistrement du run asynchrone d'un joueur pour un défi.
 */
public record RegisterChallengeRunRequest(
        @NotBlank String gameId
) {
}
