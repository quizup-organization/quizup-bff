package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Actions disponibles sur un défi, calculées côté serveur pour le joueur courant.
 */
public enum ChallengeAction {
    ACCEPT,
    DECLINE,
    CANCEL,
    PLAY
}
