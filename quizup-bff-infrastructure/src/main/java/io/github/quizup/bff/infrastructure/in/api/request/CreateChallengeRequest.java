package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Défi nominatif : le challenger vient du JWT, l'adversaire du body. */
public record CreateChallengeRequest(
        @NotBlank @Size(max = 255) String topicId,
        @NotBlank @Size(max = 255) String opponentId) {
}
