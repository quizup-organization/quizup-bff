package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.request.EnqueueMatchmakingRequest;
import io.github.quizup.matchmaking.domain.command.MatchmakingCommand;
import io.github.quizup.matchmaking.domain.model.Lobby;
import io.github.quizup.matchmaking.domain.query.LobbyQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
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

import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/matchmaking/queue} — file d'attente (ticket = lobby).
 */
@RestController
@RequestMapping("/api/matchmaking/queue")
public class MatchmakingController {

    private static final String ENDPOINT = "/api/matchmaking/queue";

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public MatchmakingController(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> enqueue(@RequestBody EnqueueMatchmakingRequest request) {
        String playerId = SecurityHelper.getUserId();
        return commandGateway
                .send(new MatchmakingCommand.EnqueuePlayerCommand(playerId, request.topicId()))
                .thenApply(ticketId -> ResponseEntityBuilder.creation(ENDPOINT, String.valueOf(ticketId)));
    }

    @GetMapping("/{ticketId}")
    public CompletableFuture<ResponseEntity<Lobby>> get(@PathVariable String ticketId) {
        return queryGateway
                .query(new LobbyQuery.FindLobbyById(ticketId), QueryResponseTypes.optionalInstanceOf(Lobby.class))
                .thenApply(optional -> optional
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @PostMapping("/{ticketId}/cancel")
    public CompletableFuture<ResponseEntity<IdResponse>> cancel(@PathVariable String ticketId) {
        String playerId = SecurityHelper.getUserId();
        return commandGateway
                .send(new MatchmakingCommand.CancelMatchmakingCommand(playerId, ticketId))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }
}
