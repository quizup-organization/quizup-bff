package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Création d'un salon privé (salle d'attente) sur un sujet. Le lien de partage est composé
 * côté client à partir de l'identifiant du salon : {@code /join/{lobbyId}}.
 * <p>
 * Si {@code opponentId} est renseigné, c'est un <b>défi nominatif</b> : seul cet invité peut
 * rejoindre (ou refuser).
 */
public record CreateLobbyRequest(
        @NotBlank String topicId,
        String opponentId
) {
}
