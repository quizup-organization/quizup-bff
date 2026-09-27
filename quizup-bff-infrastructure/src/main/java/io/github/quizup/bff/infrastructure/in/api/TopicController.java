package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.TopicViewService;
import io.github.quizup.bff.infrastructure.in.api.request.LeaderboardMonth;
import io.github.quizup.bff.infrastructure.in.api.request.LeaderboardPeriod;
import io.github.quizup.bff.infrastructure.in.api.request.LeaderboardScope;
import io.github.quizup.bff.infrastructure.in.api.request.PageParams;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.TopicCardView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicFacetsView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicLeaderboardView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicOverviewView;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.social.domain.command.TopicFollowerCommand;
import io.github.quizup.social.domain.model.FollowerIds;
import io.github.quizup.theme.domain.model.TopicCategory;
import io.github.quizup.theme.domain.model.TopicSort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/topics} — catalogue, facettes, fiche agrégée, suivi et classement.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TopicViewService topicViewService;
    private final CommandGateway commandGateway;

    public TopicController(TopicViewService topicViewService, CommandGateway commandGateway) {
        this.topicViewService = topicViewService;
        this.commandGateway = commandGateway;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<PageResponse<TopicCardView>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TopicCategory category,
            @RequestParam(defaultValue = "false") boolean followed,
            @RequestParam(defaultValue = "POPULAR") TopicSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return topicViewService
                .list(SecurityHelper.getUserId(), q, category, followed, sort, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/facets")
    public CompletableFuture<ResponseEntity<TopicFacetsView>> facets(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean followed) {
        return topicViewService
                .facets(SecurityHelper.getUserId(), q, followed)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{topicId}/overview")
    public CompletableFuture<ResponseEntity<TopicOverviewView>> overview(@PathVariable String topicId) {
        return topicViewService
                .overview(SecurityHelper.getUserId(), topicId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{topicId}/leaderboard")
    public CompletableFuture<ResponseEntity<TopicLeaderboardView>> leaderboard(
            @PathVariable String topicId,
            @RequestParam(defaultValue = "ALL_TIME") LeaderboardPeriod period,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "WORLD") LeaderboardScope scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return topicViewService
                .leaderboard(SecurityHelper.getUserId(), topicId, period, LeaderboardMonth.validate(month), scope, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{topicId}/follow")
    public CompletableFuture<ResponseEntity<Void>> follow(@PathVariable String topicId) {
        String userId = SecurityHelper.getUserId();
        return commandGateway
                .send(new TopicFollowerCommand.FollowTopicCommand(FollowerIds.topic(topicId, userId), topicId, userId))
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{topicId}/follow")
    public CompletableFuture<ResponseEntity<Void>> unfollow(@PathVariable String topicId) {
        String userId = SecurityHelper.getUserId();
        return commandGateway
                .send(new TopicFollowerCommand.UnfollowTopicCommand(FollowerIds.topic(topicId, userId), userId))
                .thenApply(_ -> ResponseEntity.noContent().build());
    }
}
