package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.MatchmakingTicketService;
import io.github.quizup.bff.infrastructure.in.api.request.EnqueueMatchmakingRequest;
import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.in.api.response.MatchmakingTicketView;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.TicketEventNotificationMapper;
import io.github.quizup.bff.infrastructure.out.messaging.response.TicketNotification;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.matchmaking.domain.query.LobbyQuery;
import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.security.SecurityHelper;
import jakarta.validation.Valid;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/matchmaking/tickets} — recherche d'adversaire : ticket, annulation, notifications.
 */
@RestController
@RequestMapping("/api/matchmaking/tickets")
public class MatchmakingController {

    private final MatchmakingTicketService matchmakingTicketService;
    private final QueryGateway queryGateway;

    public MatchmakingController(MatchmakingTicketService matchmakingTicketService, QueryGateway queryGateway) {
        this.matchmakingTicketService = matchmakingTicketService;
        this.queryGateway = queryGateway;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<MatchmakingTicketView>> enqueue(
            @Valid @RequestBody EnqueueMatchmakingRequest request) {
        return matchmakingTicketService
                .enqueue(SecurityHelper.getUserId(), request.topicId())
                .thenApply(ticket -> ResponseEntity
                        .created(URI.create("/api/matchmaking/tickets/" + ticket.ticketId()))
                        .body(ticket));
    }

    @GetMapping("/{ticketId}")
    public CompletableFuture<ResponseEntity<MatchmakingTicketView>> ticket(@PathVariable String ticketId) {
        return matchmakingTicketService
                .get(ticketId, SecurityHelper.getUserId())
                .thenApply(ticket -> ticket
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @PostMapping("/{ticketId}/cancel")
    public CompletableFuture<ResponseEntity<Void>> cancel(@PathVariable String ticketId) {
        return matchmakingTicketService
                .cancel(SecurityHelper.getUserId(), ticketId)
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    /**
     * Historique des notifications du ticket (même contrat que le push WebSocket) : le service
     * matchmaking expose son event store en {@code EventEnvelope} typés, le BFF produit le contrat
     * web ({@link EventEnvelopeResponse}).
     */
    @GetMapping("/{ticketId}/notifications")
    public CompletableFuture<ResponseEntity<List<EventEnvelopeResponse>>> notifications(
            @PathVariable String ticketId) {
        return queryGateway
                .query(new LobbyQuery.GetLobbyEventsQuery(ticketId),
                        QueryResponseTypes.multipleInstancesOf(EventEnvelope.class))
                .thenApply(envelopes -> envelopes.stream()
                        .map(MatchmakingController::toResponse)
                        .filter(Objects::nonNull)
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    private static EventEnvelopeResponse toResponse(EventEnvelope envelope) {
        if (!(envelope.payload() instanceof LobbyEvent event)) {
            return null;
        }
        TicketNotification notification = TicketEventNotificationMapper.toNotification(event);
        if (notification == null) {
            return null;
        }
        return EventEnvelopeResponse.of(
                envelope.aggregateId(),
                envelope.sequenceNumber(),
                envelope.timestamp(),
                notification.type().name(),
                notification);
    }
}
