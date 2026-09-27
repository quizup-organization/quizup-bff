package io.github.quizup.bff.infrastructure.in.api.response;

import java.time.Instant;

/**
 * Ticket de matchmaking : état de la recherche d'adversaire, du point de vue du joueur courant.
 * Le statut expose le langage produit ({@code SEARCHING} / {@code MATCHED} / {@code CANCELLED}),
 * pas le statut interne du lobby.
 */
public record MatchmakingTicketView(
        String ticketId,
        String topicId,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        String gameId,
        String opponentId,
        boolean vsBot
) {

    public enum Status {
        SEARCHING,
        MATCHED,
        CANCELLED
    }
}
