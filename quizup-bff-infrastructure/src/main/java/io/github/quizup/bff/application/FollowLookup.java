package io.github.quizup.bff.application;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.social.domain.model.FollowDirection;
import io.github.quizup.social.domain.model.TopicFollower;
import io.github.quizup.social.domain.model.UserFollowCounts;
import io.github.quizup.social.domain.model.UserFollower;
import io.github.quizup.social.domain.query.TopicFollowerQuery;
import io.github.quizup.social.domain.query.UserFollowerQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lecture centralisée des follows (sujets et joueurs) pour les compositions BFF.
 * Les listes sont bornées (pas de pagination profonde côté social) ; le BFF découpe ensuite
 * ses propres pages.
 */
@Service
public class FollowLookup {

    public static final int MAX_LIST_SIZE = 200;

    private final QueryGateway queryGateway;

    public FollowLookup(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    /** Identifiants des sujets suivis, plus récents d'abord. */
    public CompletableFuture<List<String>> followedTopicIds(String userId, int limit) {
        return queryGateway
                .query(new TopicFollowerQuery.GetTopicFollowsQuery(userId, limit),
                        QueryResponseTypes.multipleInstancesOf(TopicFollower.class))
                .thenApply(follows -> follows.stream().map(TopicFollower::topicId).toList());
    }

    /** Identifiants des joueurs suivis, plus récents d'abord. */
    public CompletableFuture<List<String>> followingIds(String userId, int limit) {
        return queryGateway
                .query(new UserFollowerQuery.GetUserFollowsQuery(userId, FollowDirection.FOLLOWING, limit),
                        QueryResponseTypes.multipleInstancesOf(UserFollower.class))
                .thenApply(follows -> follows.stream().map(UserFollower::followedId).toList());
    }

    public CompletableFuture<UserFollowCounts> userCounts(String userId) {
        return queryGateway.query(
                new UserFollowerQuery.GetUserFollowCountsQuery(userId),
                QueryResponseTypes.instanceOf(UserFollowCounts.class));
    }

    public CompletableFuture<Boolean> userFollowed(String followerId, String followedId) {
        return queryGateway.query(
                new UserFollowerQuery.ExistsUserFollowerQuery(followerId, followedId),
                QueryResponseTypes.instanceOf(Boolean.class));
    }

    public CompletableFuture<Boolean> topicFollowed(String topicId, String userId) {
        return queryGateway.query(
                new TopicFollowerQuery.ExistsTopicFollowerQuery(topicId, userId),
                QueryResponseTypes.instanceOf(Boolean.class));
    }
}
