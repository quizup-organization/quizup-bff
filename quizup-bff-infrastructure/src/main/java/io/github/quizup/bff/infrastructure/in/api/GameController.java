package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.bff.infrastructure.in.api.request.AnswerQuestionRequest;
import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.bff.infrastructure.out.messaging.response.GameNotification;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.security.SecurityHelper;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/games} — parties (bot / asynchrone), lecture et commandes.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private static final String ENDPOINT = "/api/games";

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public GameController(QueryGateway queryGateway, CommandGateway commandGateway, ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<Game>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return queryGateway
                .query(
                        new GameQuery.SearchGameQuery(criteria.filters(), criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(Game.class)
                )
                .thenApply(PageMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{gameId}")
    public CompletableFuture<ResponseEntity<Game>> getById(@PathVariable String gameId) {
        return queryGateway
                .query(new GameQuery.GetGameByIdQuery(gameId), QueryResponseTypes.instanceOf(Game.class))
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Historique des notifications d'une partie (même contrat que le push WebSocket) : le client
     * bootstrap son read model puis déduplique par {@code sequenceNumber}.
     */
    @GetMapping("/{gameId}/notifications")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<ResponseEntity<List<NotificationEnvelope<GameNotification>>>> notifications(
            @PathVariable String gameId) {
        return queryGateway
                .query(new GameQuery.GetGameEventsQuery(gameId), QueryResponseTypes.multipleInstancesOf(NotificationEnvelope.class))
                .thenApply(envelopes -> envelopes.stream()
                        .map(envelope -> toGameNotificationEnvelope((NotificationEnvelope) envelope))
                        .flatMap(Optional::stream)
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    @SuppressWarnings("rawtypes")
    private static Optional<NotificationEnvelope<GameNotification>> toGameNotificationEnvelope(
            NotificationEnvelope envelope) {
        if (!(envelope.payload() instanceof GameEvent event)) {
            return Optional.empty();
        }
        return GameEventNotificationMapper.toNotification(event)
                .map(notification -> new NotificationEnvelope<>(
                        envelope.notificationId(),
                        envelope.aggregateId(),
                        envelope.sequenceNumber(),
                        envelope.occurredAt(),
                        notification
                ));
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> create(@RequestBody CreateGameRequest request) {
        String gameId = UUID.randomUUID().toString();
        String playerId = SecurityHelper.getUserId();
        boolean async = "ASYNC".equalsIgnoreCase(request.mode());
        return displayName(playerId).thenCompose(playerName -> commandGateway
                .send(new GameCommand.CreateGameCommand(
                        gameId,
                        request.topicId(),
                        playerId,
                        playerName,
                        createOpponentId(async, request),
                        createOpponentName(async, request),
                        async ? GameMode.ASYNC : GameMode.SYNC,
                        async ? createdPlayerType(request) : GamePlayerType.BOT,
                        async ? null : BotDifficulty.fromOrDefault(parseDifficulty(request.difficulty())),
                        async ? request.ghostGameId() : null
                ))
                .thenApply(_ -> ResponseEntityBuilder.creation(ENDPOINT, gameId)));
    }

    @PostMapping("/{gameId}/answer")
    public CompletableFuture<ResponseEntity<IdResponse>> answer(
            @PathVariable String gameId,
            @RequestBody AnswerQuestionRequest request) {
        return commandGateway
                .send(new GameCommand.AnswerQuestionCommand(
                        gameId,
                        SecurityHelper.getUserId(),
                        GameQuestionChoice.valueOf(request.choice()),
                        Instant.now()
                ))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }

    @PostMapping("/{gameId}/cancel")
    public CompletableFuture<ResponseEntity<IdResponse>> cancel(@PathVariable String gameId) {
        return commandGateway
                .send(new GameCommand.CancelGameCommand(gameId, "PLAYER_CANCELLED"))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }

    @PostMapping("/{gameId}/abandon")
    public CompletableFuture<ResponseEntity<IdResponse>> abandon(@PathVariable String gameId) {
        return commandGateway
                .send(new GameCommand.EndGameCommand(gameId, SecurityHelper.getUserId()))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }

    private static boolean isReplay(CreateGameRequest request) {
        return request.ghostGameId() != null && !request.ghostGameId().isBlank();
    }

    private static String createOpponentId(boolean async, CreateGameRequest request) {
        if (!async) {
            return QuizUpConstants.SYSTEM_USER_ID;
        }
        return isReplay(request) ? request.opponentId() : null;
    }

    private static String createOpponentName(boolean async, CreateGameRequest request) {
        if (!async) {
            return QuizUpConstants.SYSTEM_USER_NAME;
        }
        return isReplay(request) ? request.opponentName() : null;
    }

    private static GamePlayerType createdPlayerType(CreateGameRequest request) {
        return isReplay(request) ? GamePlayerType.GHOST : GamePlayerType.HUMAN;
    }

    private static BotDifficulty parseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return null;
        }
        try {
            return BotDifficulty.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private CompletableFuture<String> displayName(String userId) {
        return profileLookup.get(userId).thenApply(profile -> profile.displayName());
    }
}
