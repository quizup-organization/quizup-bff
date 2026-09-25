package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.request.CreateChallengeRequest;
import io.github.quizup.bff.infrastructure.in.api.request.RegisterChallengeRunRequest;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.social.domain.command.ChallengeCommand;
import io.github.quizup.social.domain.model.Challenge;
import io.github.quizup.social.domain.query.ChallengeQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/challenges} — défis 1v1 (lecture + transitions d'état).
 */
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private static final String ENDPOINT = "/api/challenges";

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public ChallengeController(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<SearchResponse<Challenge>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        return queryGateway
                .query(new ChallengeQuery.SearchChallengeQuery(searchRequest), QueryResponseTypes.searchResponseOf(Challenge.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{challengeId}")
    public CompletableFuture<ResponseEntity<Challenge>> getById(@PathVariable String challengeId) {
        return queryGateway
                .query(new ChallengeQuery.GetChallengeByIdQuery(challengeId), QueryResponseTypes.instanceOf(Challenge.class))
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> create(@RequestBody CreateChallengeRequest request) {
        String challengeId = UUID.randomUUID().toString();
        String challengerId = SecurityHelper.getUserId();
        return commandGateway
                .send(new ChallengeCommand.CreateChallengeCommand(challengeId, challengerId, request.challengedId(), request.topicId()))
                .thenApply(_ -> ResponseEntityBuilder.creation(ENDPOINT, challengeId));
    }

    @PostMapping("/{challengeId}/accept")
    public CompletableFuture<ResponseEntity<IdResponse>> accept(@PathVariable String challengeId) {
        return commandGateway
                .send(new ChallengeCommand.AcceptChallengeCommand(challengeId, SecurityHelper.getUserId()))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }

    @PostMapping("/{challengeId}/decline")
    public CompletableFuture<ResponseEntity<IdResponse>> decline(@PathVariable String challengeId) {
        return commandGateway
                .send(new ChallengeCommand.DeclineChallengeCommand(challengeId, SecurityHelper.getUserId()))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }

    @PostMapping("/{challengeId}/cancel")
    public CompletableFuture<ResponseEntity<IdResponse>> cancel(@PathVariable String challengeId) {
        return commandGateway
                .send(new ChallengeCommand.CancelChallengeCommand(challengeId, SecurityHelper.getUserId()))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }

    @PostMapping("/{challengeId}/runs")
    public CompletableFuture<ResponseEntity<IdResponse>> registerRun(
            @PathVariable String challengeId,
            @RequestBody RegisterChallengeRunRequest request) {
        return commandGateway
                .send(new ChallengeCommand.RegisterChallengeRunCommand(challengeId, SecurityHelper.getUserId(), request.gameId()))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }
}
