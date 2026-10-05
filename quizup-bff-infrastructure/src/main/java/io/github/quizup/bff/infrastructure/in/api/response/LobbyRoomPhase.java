package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Phase de la salle temps réel, dérivée du read model : attente d'un participant (lien),
 * attente de présence, compte à rebours, partie créée, absence ou clôture.
 */
public enum LobbyRoomPhase {
    WAITING_PARTICIPANT,
    WAITING_PRESENCE,
    READY,
    COMPLETED,
    MISSED,
    CLOSED,
    FAILED
}
