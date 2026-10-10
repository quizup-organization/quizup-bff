package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Phase de la salle, dérivée du read model : attente du second participant (lien), attente de
 * présence, compte à rebours, partie créée, clôture ou échec de préparation.
 */
public enum RoomPhase {
    WAITING_PARTICIPANT,
    WAITING_PRESENCE,
    READY,
    COMPLETED,
    CLOSED,
    FAILED
}
