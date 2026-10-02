package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Création d'un salon privé (salle d'attente) sur un sujet. Le lien de partage est composé
 * côté client à partir de l'identifiant du salon : {@code /join/{lobbyId}}.
 */
public record CreateLobbyRequest(
        @NotBlank String topicId
) {
}
