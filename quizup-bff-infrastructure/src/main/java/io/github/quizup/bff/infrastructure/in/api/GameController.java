package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.GameViewService;
import io.github.quizup.bff.infrastructure.in.api.request.AnswerQuestionRequest;
import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.in.api.response.CurrentGameView;
import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.query.GameQuery;
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
 * {@code /api/games} — arène : création, réponse, abandon, historique de notifications.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private static final String ENDPOINT = "/api/games";

    private final GameViewService gameViewService;
    private final QueryGateway queryGateway;

    public GameController(GameViewService gameViewService, QueryGateway queryGateway) {
        this.gameViewService = gameViewService;
        this.queryGateway = queryGateway;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> create(@Valid @RequestBody CreateGameRequest request) {
        return gameViewService
                .createBotGame(SecurityHelper.getUserId(), request)
                .thenApply(gameId -> ResponseEntityBuilder.creation(ENDPOINT, gameId));
    }

    /** Partie en attente/en cours du joueur (reprise) ; 204 s'il n'y en a aucune. */
    @GetMapping("/current")
    public CompletableFuture<ResponseEntity<CurrentGameView>> current() {
        return gameViewService
                .current(SecurityHelper.getUserId())
                .thenApply(view -> view == null
                        ? ResponseEntity.<CurrentGameView>noContent().build()
                        : ResponseEntity.ok(view));
    }

    @PostMapping("/{gameId}/join")
    public CompletableFuture<ResponseEntity<Void>> join(@PathVariable String gameId) {
        return gameViewService
                .join(gameId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{gameId}/leave")
    public CompletableFuture<ResponseEntity<Void>> leave(@PathVariable String gameId) {
        return gameViewService
                .leave(gameId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{gameId}/answer")
    public CompletableFuture<ResponseEntity<Void>> answer(@PathVariable String gameId,
                                                          @Valid @RequestBody AnswerQuestionRequest request) {
        return gameViewService
                .answer(gameId, SecurityHelper.getUserId(), request.choice())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{gameId}/abandon")
    public CompletableFuture<ResponseEntity<Void>> abandon(@PathVariable String gameId) {
        return gameViewService
                .abandon(gameId, SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/{gameId}/cancel")
    public CompletableFuture<ResponseEntity<Void>> cancel(@PathVariable String gameId) {
        return gameViewService
                .cancel(gameId)
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    /** Résultat d'une partie terminée : score, détail, récompense XP et progression du joueur. */
    @GetMapping("/{gameId}/result")
    public CompletableFuture<ResponseEntity<GameResultView>> result(@PathVariable String gameId) {
        return gameViewService
                .result(gameId, SecurityHelper.getUserId())
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Historique des notifications d'une partie (même contrat que le push WebSocket) : le client
     * bootstrap son read model puis déduplique par {@code sequenceNumber}. Le service game mappe
     * son event store en {@code EventEnvelope} typés ; le BFF produit le contrat web
     * ({@link EventEnvelopeResponse}).
     */
    @GetMapping("/{gameId}/notifications")
    public CompletableFuture<ResponseEntity<List<EventEnvelopeResponse>>> notifications(
            @PathVariable String gameId) {
        return queryGateway
                .query(new GameQuery.GetGameEventsQuery(gameId),
                        QueryResponseTypes.multipleInstancesOf(EventEnvelope.class))
                .thenApply(envelopes -> envelopes.stream()
                        .map(GameController::toResponse)
                        .flatMap(Optional::stream)
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    private static Optional<EventEnvelopeResponse> toResponse(EventEnvelope envelope) {
        if (!(envelope.payload() instanceof GameEvent event)) {
            return Optional.empty();
        }
        return GameEventNotificationMapper.toNotification(event)
                .map(notification -> EventEnvelopeResponse.of(
                        envelope.aggregateId(),
                        envelope.sequenceNumber(),
                        envelope.timestamp(),
                        notification.type().name(),
                        notification));
    }
}
