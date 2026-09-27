package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.HomeView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicCardView;
import io.github.quizup.game.domain.model.TopicPopularity;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Composition de l'accueil : sujets suivis (récents) + sujets les plus joués (30 derniers jours).
 */
@Service
public class HomeService {

    private static final int FOLLOWED_LIMIT = 10;
    private static final int TRENDING_LIMIT = 10;
    private static final Duration TRENDING_WINDOW = Duration.ofDays(30);

    private final QueryGateway queryGateway;
    private final FollowLookup followLookup;

    public HomeService(QueryGateway queryGateway, FollowLookup followLookup) {
        this.queryGateway = queryGateway;
        this.followLookup = followLookup;
    }

    public CompletableFuture<HomeView> home(String userId) {
        CompletableFuture<List<String>> followedIdsFuture =
                followLookup.followedTopicIds(userId, FollowLookup.MAX_LIST_SIZE);

        return followedIdsFuture.thenCompose(followedIds -> {
            List<String> recentFollowedIds = followedIds.stream().limit(FOLLOWED_LIMIT).toList();

            CompletableFuture<List<Topic>> followedTopicsFuture = recentFollowedIds.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : queryGateway.query(new TopicQuery.GetTopicsByIdsQuery(recentFollowedIds),
                            QueryResponseTypes.multipleInstancesOf(Topic.class));
            CompletableFuture<List<TopicPopularity>> popularFuture = queryGateway.query(
                    new GameQuery.GetPopularTopicsQuery(Instant.now().minus(TRENDING_WINDOW), TRENDING_LIMIT),
                    QueryResponseTypes.multipleInstancesOf(TopicPopularity.class));

            return CompletableFuture.allOf(followedTopicsFuture, popularFuture).thenCompose(_ -> {
                List<String> trendingIds = popularFuture.join().stream()
                        .map(TopicPopularity::topicId)
                        .toList();
                CompletableFuture<List<Topic>> trendingTopicsFuture = trendingIds.isEmpty()
                        ? CompletableFuture.completedFuture(List.of())
                        : queryGateway.query(new TopicQuery.GetTopicsByIdsQuery(trendingIds),
                                QueryResponseTypes.multipleInstancesOf(Topic.class));

                return trendingTopicsFuture.thenApply(trendingTopics -> {
                    Set<String> followed = Set.copyOf(followedIds);
                    List<TopicCardView> followedTopics = followedTopicsFuture.join().stream()
                            .map(topic -> TopicViews.toCard(topic, true))
                            .toList();
                    List<TopicCardView> trending = trendingTopics.stream()
                            .map(topic -> TopicViews.toCard(topic, followed.contains(topic.topicId())))
                            .toList();
                    return new HomeView(followedTopics, trending);
                });
            });
        });
    }
}
