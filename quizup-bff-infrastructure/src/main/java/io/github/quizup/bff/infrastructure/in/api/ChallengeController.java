package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ChallengeViewService;
import io.github.quizup.bff.infrastructure.in.api.request.CreateChallengeRequest;
import io.github.quizup.bff.infrastructure.in.api.request.PageParams;
import io.github.quizup.bff.infrastructure.in.api.request.RegisterChallengeRunRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeCardView;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeDetailView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PendingCountView;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.social.domain.command.ChallengeCommand;
import io.github.quizup.social.domain.model.ChallengeBox;
import io.github.quizup.social.domain.model.ChallengeStatus;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/challenges} — défis 1v1 : liste enrichie, détail, compteur, transitions.
 */
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private static final String ENDPOINT = "/api/challenges";
    private static final int MAX_PAGE_SIZE = 100;

    private final ChallengeViewService challengeViewService;
    private final CommandGateway commandGateway;

    public ChallengeController(ChallengeViewService challengeViewService, CommandGateway commandGateway) {
        this.challengeViewService = challengeViewService;
        this.commandGateway = commandGateway;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<PageResponse<ChallengeCardView>>> list(
            @RequestParam(defaultValue = "ALL") ChallengeBox box,
            @RequestParam(required = false) ChallengeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return challengeViewService
                .list(SecurityHelper.getUserId(), box, status, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/pending-count")
    public CompletableFuture<ResponseEntity<PendingCountView>> pendingCount() {
        return challengeViewService
                .pendingCount(SecurityHelper.getUserId())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{challengeId}")
    public CompletableFuture<ResponseEntity<ChallengeDetailView>> detail(@PathVariable String challengeId) {
        return challengeViewService
                .detail(SecurityHelper.getUserId(), challengeId)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> create(@Valid @RequestBody CreateChallengeRequest request) {
        String challengeId = UUID.randomUUID().toString();
        return commandGateway
                .send(new ChallengeCommand.CreateChallengeCommand(
                        challengeId,
                        SecurityHelper.getUserId(),
                        request.challengedId(),
                        request.topicId()))
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
            @Valid @RequestBody RegisterChallengeRunRequest request) {
        return commandGateway
                .send(new ChallengeCommand.RegisterChallengeRunCommand(
                        challengeId,
                        SecurityHelper.getUserId(),
                        request.gameId()))
                .thenApply(id -> ResponseEntityBuilder.ok(String.valueOf(id)));
    }
}
