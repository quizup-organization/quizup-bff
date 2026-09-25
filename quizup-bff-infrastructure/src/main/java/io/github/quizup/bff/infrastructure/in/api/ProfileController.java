package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.mapper.ProgressionResponseMapper;
import io.github.quizup.bff.infrastructure.in.api.request.UpdateProfileRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionResponse;
import io.github.quizup.bff.infrastructure.in.api.response.TopicProgressResponse;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.ActivityView;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.query.ActivityQuery;
import io.github.quizup.profile.domain.query.ProfileQuery;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/profiles} — profils publics et progression, via le bus de requêtes distribué.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final int DEFAULT_ACTIVITY_WINDOW_DAYS = 364;

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public ProfileController(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<SearchResponse<Profile>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        return queryGateway
                .query(new ProfileQuery.ProfileSearchQuery(searchRequest), QueryResponseTypes.searchResponseOf(Profile.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<Profile>> getById(@PathVariable String userId) {
        return queryGateway
                .query(new ProfileQuery.GetProfileQuery(userId), QueryResponseTypes.instanceOf(Profile.class))
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{userId}")
    public CompletableFuture<ResponseEntity<Void>> update(
            @PathVariable String userId,
            @RequestBody UpdateProfileRequest request) {
        return commandGateway
                .send(new ProfileCommand.UpdateProfileCommand(
                        userId,
                        SecurityHelper.getUserId(),
                        request.displayName(),
                        request.bio(),
                        request.country(),
                        request.avatarOptions()))
                .thenApply(updatedId -> ResponseEntity.ok().build());
    }

    @GetMapping("/{userId}/progress")
    public CompletableFuture<ResponseEntity<ProgressionResponse>> getProgress(@PathVariable String userId) {
        return queryGateway
                .query(new ProgressionQuery.GetProgressionQuery(userId), QueryResponseTypes.instanceOf(PlayerProgress.class))
                .thenApply(ProgressionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}/progress/{topicId}")
    public CompletableFuture<ResponseEntity<TopicProgressResponse>> getTopicProgress(
            @PathVariable String userId,
            @PathVariable String topicId) {
        return queryGateway
                .query(
                        new ProgressionQuery.GetTopicProgressionQuery(userId, topicId),
                        QueryResponseTypes.instanceOf(TopicProgress.class)
                )
                .thenApply(ProgressionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}/activity")
    public CompletableFuture<ResponseEntity<ActivityView>> getActivity(
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_ACTIVITY_WINDOW_DAYS);
        return queryGateway
                .query(
                        new ActivityQuery.GetActivityQuery(userId, start, end),
                        QueryResponseTypes.instanceOf(ActivityView.class)
                )
                .thenApply(ResponseEntity::ok);
    }
}
