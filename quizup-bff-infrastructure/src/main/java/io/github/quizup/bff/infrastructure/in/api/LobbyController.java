package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.out.messaging.mapper.LobbyEventNotificationMapper;
import io.github.quizup.bff.infrastructure.out.messaging.response.LobbyNotification;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.matchmaking.domain.query.LobbyQuery;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/lobbies} — historique des notifications d'un lobby (même contrat que le push).
 */
@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    private final QueryGateway queryGateway;

    public LobbyController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/{lobbyId}/notifications")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<ResponseEntity<List<NotificationEnvelope<LobbyNotification>>>> notifications(
            @PathVariable String lobbyId) {
        return queryGateway
                .query(new LobbyQuery.GetLobbyEventsQuery(lobbyId), QueryResponseTypes.multipleInstancesOf(NotificationEnvelope.class))
                .thenApply(envelopes -> envelopes.stream()
                        .map(envelope -> toLobbyNotificationEnvelope((NotificationEnvelope) envelope))
                        .flatMap(Optional::stream)
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    @SuppressWarnings("rawtypes")
    private static Optional<NotificationEnvelope<LobbyNotification>> toLobbyNotificationEnvelope(
            NotificationEnvelope envelope) {
        if (!(envelope.payload() instanceof LobbyEvent event)) {
            return Optional.empty();
        }
        return LobbyEventNotificationMapper.toNotification(event)
                .map(notification -> new NotificationEnvelope<>(
                        envelope.notificationId(),
                        envelope.aggregateId(),
                        envelope.sequenceNumber(),
                        envelope.occurredAt(),
                        notification
                ));
    }
}
