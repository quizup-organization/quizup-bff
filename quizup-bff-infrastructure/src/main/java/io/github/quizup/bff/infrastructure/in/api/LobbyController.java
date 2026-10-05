package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.LobbyViewService;
import io.github.quizup.bff.infrastructure.in.api.request.CreateLobbyRequest;
import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.in.api.response.LobbyView;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.LobbyEventNotificationMapper;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.matchmaking.domain.query.LobbyQuery;
import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/lobbies} — salons privés : création, consultation, présence et annulation.
 * Le lien de partage est composé côté client : {@code /join/{lobbyId}}.
 */
@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    private static final String ENDPOINT = "/api/lobbies";

    private final LobbyViewService lobbyViewService;
    private final QueryGateway queryGateway;

    public LobbyController(LobbyViewService lobbyViewService, QueryGateway queryGateway) {
        this.lobbyViewService = lobbyViewService;
        this.queryGateway = queryGateway;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> create(@Valid @RequestBody CreateLobbyRequest request) {
        return lobbyViewService
                .create(SecurityHelper.getUserId(), request)
                .thenApply(lobbyId -> ResponseEntityBuilder.creation(ENDPOINT, lobbyId));
    }

    @GetMapping("/mine")
    public CompletableFuture<ResponseEntity<List<LobbyView>>> mine() {
        return lobbyViewService.mine(SecurityHelper.getUserId()).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{lobbyId}")
    public CompletableFuture<ResponseEntity<LobbyView>> get(@PathVariable String lobbyId) {
        return lobbyViewService.get(lobbyId, SecurityHelper.getUserId()).thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{lobbyId}/join")
    public CompletableFuture<ResponseEntity<Void>> join(@PathVariable String lobbyId) {
        return lobbyViewService
                .join(lobbyId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    /** Entrée effective dans la salle (présence temps réel, idempotent). */
    @PostMapping("/{lobbyId}/enter")
    public CompletableFuture<ResponseEntity<Void>> enter(@PathVariable String lobbyId) {
        return lobbyViewService
                .enter(lobbyId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{lobbyId}/leave")
    public CompletableFuture<ResponseEntity<Void>> leave(@PathVariable String lobbyId) {
        return lobbyViewService
                .leave(lobbyId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{lobbyId}/cancel")
    public CompletableFuture<ResponseEntity<Void>> cancel(@PathVariable String lobbyId) {
        return lobbyViewService
                .cancel(lobbyId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{lobbyId}/decline")
    public CompletableFuture<ResponseEntity<Void>> decline(@PathVariable String lobbyId) {
        return lobbyViewService
                .decline(lobbyId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @GetMapping("/{lobbyId}/notifications")
    public CompletableFuture<ResponseEntity<List<EventEnvelopeResponse>>> notifications(@PathVariable String lobbyId) {
        return queryGateway
                .query(new LobbyQuery.GetLobbyEventsQuery(lobbyId),
                        QueryResponseTypes.multipleInstancesOf(EventEnvelope.class))
                .thenApply(envelopes -> envelopes.stream()
                        .map(LobbyController::toResponse)
                        .flatMap(Optional::stream)
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    private static Optional<EventEnvelopeResponse> toResponse(EventEnvelope envelope) {
        if (!(envelope.payload() instanceof LobbyEvent event)) {
            return Optional.empty();
        }
        return LobbyEventNotificationMapper.toNotification(event)
                .map(notification -> EventEnvelopeResponse.of(
                        envelope.aggregateId(),
                        envelope.sequenceNumber(),
                        envelope.timestamp(),
                        notification.type().name(),
                        notification));
    }
}
