package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.LeaderboardScopeService;
import io.github.quizup.bff.application.LeaderboardScopeService.ScopeFilter;
import io.github.quizup.bff.infrastructure.in.api.response.TopicLeaderboardEntryView;
import io.github.quizup.leaderboard.domain.model.LeaderboardRank;
import io.github.quizup.leaderboard.domain.model.LeaderboardRules;
import io.github.quizup.leaderboard.domain.model.TopicLeaderboardEntry;
import io.github.quizup.leaderboard.domain.query.LeaderboardQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.security.SecurityHelper;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Sous-ressource {@code /api/topics/{topicId}/leaderboard[/me]} — classement par sujet.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicLeaderboardController {

    private final QueryGateway queryGateway;
    private final LeaderboardScopeService scopeService;

    public TopicLeaderboardController(QueryGateway queryGateway, LeaderboardScopeService scopeService) {
        this.queryGateway = queryGateway;
        this.scopeService = scopeService;
    }

    @GetMapping("/{topicId}/leaderboard")
    public CompletableFuture<ResponseEntity<List<TopicLeaderboardEntryView>>> top(
            @PathVariable String topicId,
            @RequestParam(defaultValue = "all-time") String period,
            @RequestParam(defaultValue = "world") String scope,
            @RequestParam(defaultValue = "50") int limit) {
        // Portée `world` : le demandeur est optionnel (ex. token M2M) ; `following`/`country` le requièrent.
        ScopeFilter filter = scopeService.resolve(scope, SecurityHelper.findUserId().orElse(null));
        return queryGateway
                .query(
                        new LeaderboardQuery.TopByTopicQuery(
                                topicId, isMonthly(period), LeaderboardRules.currentMonth(), limit,
                                filter.memberIds(), filter.country()),
                        QueryResponseTypes.multipleInstancesOf(TopicLeaderboardEntry.class)
                )
                .thenApply(entries -> IntStream.range(0, entries.size())
                        .mapToObj(index -> toView(entries.get(index), index + 1))
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{topicId}/leaderboard/me")
    public CompletableFuture<ResponseEntity<TopicLeaderboardEntryView>> me(
            @PathVariable String topicId,
            @RequestParam(defaultValue = "all-time") String period,
            @RequestParam(defaultValue = "world") String scope) {
        String userId = SecurityHelper.getUserId();
        ScopeFilter filter = scopeService.resolve(scope, userId);
        return queryGateway
                .query(
                        new LeaderboardQuery.GetTopicRankQuery(
                                topicId, userId, isMonthly(period), LeaderboardRules.currentMonth(),
                                filter.memberIds(), filter.country()),
                        QueryResponseTypes.optionalInstanceOf(LeaderboardRank.class)
                )
                .thenApply(optional -> optional
                        .map(rank -> ResponseEntity.ok(toView(rank.entry(), rank.rank())))
                        .orElseGet(() -> ResponseEntity.noContent().build()));
    }

    private boolean isMonthly(String period) {
        return "monthly".equals(period);
    }

    private static TopicLeaderboardEntryView toView(TopicLeaderboardEntry entry, int rank) {
        return new TopicLeaderboardEntryView(
                rank,
                entry.topicId(),
                entry.userId(),
                entry.displayName(),
                entry.country(),
                entry.totalXp(),
                entry.monthlyXp(),
                entry.level()
        );
    }
}
