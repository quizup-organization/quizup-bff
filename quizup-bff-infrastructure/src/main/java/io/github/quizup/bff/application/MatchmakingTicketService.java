package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.MatchmakingTicketView;
import io.github.quizup.matchmaking.domain.command.MatchmakingCommand;
import io.github.quizup.matchmaking.domain.model.MatchmakingTicket;
import io.github.quizup.matchmaking.domain.query.TicketQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Matchmaking web : la façade lit le **read model ticket** du service matchmaking (statuts produit
 * explicites), plus aucun mapping depuis le lobby interne.
 */
@Service
public class MatchmakingTicketService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public MatchmakingTicketService(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    /**
     * Entrée en file. La vue retournée est construite depuis l'issue de la commande (le ticket
     * vient d'être ouvert, la projection peut être en retard) : statut {@code SEARCHING}.
     */
    public CompletableFuture<MatchmakingTicketView> enqueue(String playerId, String topicId) {
        return commandGateway
                .send(new MatchmakingCommand.EnqueuePlayerCommand(playerId, topicId))
                .thenApply(ticketId -> new MatchmakingTicketView(
                        String.valueOf(ticketId),
                        topicId,
                        MatchmakingTicketView.Status.SEARCHING,
                        Instant.now(),
                        Instant.now(),
                        null,
                        null,
                        false));
    }

    public CompletableFuture<Optional<MatchmakingTicketView>> get(String ticketId, String requesterId) {
        return queryGateway
                .query(new TicketQuery.FindTicketByIdQuery(ticketId),
                        QueryResponseTypes.optionalInstanceOf(MatchmakingTicket.class))
                .thenApply(ticket -> ticket.map(value -> toView(value, requesterId)));
    }

    public CompletableFuture<Void> cancel(String playerId, String ticketId) {
        return commandGateway
                .send(new MatchmakingCommand.CancelMatchmakingCommand(playerId, ticketId))
                .thenAccept(_ -> {
                });
    }

    public static MatchmakingTicketView toView(MatchmakingTicket ticket, String requesterId) {
        MatchmakingTicketView.Status status = switch (ticket.status()) {
            case SEARCHING -> MatchmakingTicketView.Status.SEARCHING;
            case MATCHED -> MatchmakingTicketView.Status.MATCHED;
            case CANCELLED -> MatchmakingTicketView.Status.CANCELLED;
        };
        String opponentId = status == MatchmakingTicketView.Status.MATCHED
                ? opponentOf(ticket, requesterId)
                : null;
        return new MatchmakingTicketView(
                ticket.ticketId(),
                ticket.topicId(),
                status,
                ticket.createdAt(),
                ticket.updatedAt(),
                ticket.gameId(),
                opponentId,
                ticket.vsBot());
    }

    private static String opponentOf(MatchmakingTicket ticket, String requesterId) {
        if (requesterId != null && requesterId.equals(ticket.opponentId())) {
            return ticket.initiatorId();
        }
        return ticket.opponentId();
    }
}
