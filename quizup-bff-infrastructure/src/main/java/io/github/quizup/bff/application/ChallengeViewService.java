package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.ChallengeAction;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeCardView;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeDetailView;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeDirection;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PendingCountView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.social.domain.model.Challenge;
import io.github.quizup.social.domain.model.ChallengeBox;
import io.github.quizup.social.domain.model.ChallengePage;
import io.github.quizup.social.domain.model.ChallengeStatus;
import io.github.quizup.social.domain.query.ChallengeQuery;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Composition des vues défis : liste (reçus/envoyés), détail, compteur d'attente.
 */
@Service
public class ChallengeViewService {

    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;

    public ChallengeViewService(QueryGateway queryGateway, ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
    }

    public CompletableFuture<PageResponse<ChallengeCardView>> list(String viewerId,
                                                                   ChallengeBox box,
                                                                   ChallengeStatus status,
                                                                   int page,
                                                                   int size) {
        return queryGateway.query(
                        new ChallengeQuery.GetChallengeBoxQuery(viewerId, box, status, page, size),
                        QueryResponseTypes.instanceOf(ChallengePage.class))
                .thenCompose(result -> enrich(viewerId, result.challenges())
                        .thenApply(cards -> PageResponse.of(cards, result.page(), result.size(), result.totalElements())));
    }

    public CompletableFuture<PendingCountView> pendingCount(String viewerId) {
        return queryGateway.query(
                        new ChallengeQuery.CountPendingChallengesQuery(viewerId),
                        QueryResponseTypes.instanceOf(Long.class))
                .thenApply(PendingCountView::new);
    }

    public CompletableFuture<ChallengeDetailView> detail(String viewerId, String challengeId) {
        return queryGateway.query(
                        new ChallengeQuery.GetChallengeByIdQuery(challengeId),
                        QueryResponseTypes.instanceOf(Challenge.class))
                .thenCompose(challenge -> {
                    List<String> userIds = List.of(challenge.challengerId(), challenge.challengedId());
                    CompletableFuture<List<Profile>> profilesFuture = profileLookup.getAll(userIds);
                    CompletableFuture<List<Topic>> topicsFuture = queryGateway.query(
                            new TopicQuery.GetTopicsByIdsQuery(List.of(challenge.topicId())),
                            QueryResponseTypes.multipleInstancesOf(Topic.class));
                    return CompletableFuture.allOf(profilesFuture, topicsFuture)
                            .thenApply(_ -> {
                                Map<String, Profile> profileById = byId(profilesFuture.join());
                                Topic topic = topicsFuture.join().stream().findFirst().orElse(null);
                                boolean isChallenger = viewerId.equals(challenge.challengerId());
                                String myRunGameId = isChallenger
                                        ? challenge.challengerGameId()
                                        : challenge.challengedGameId();
                                String opponentRunGameId = isChallenger
                                        ? challenge.challengedGameId()
                                        : challenge.challengerGameId();
                                return new ChallengeDetailView(
                                        challenge.challengeId(),
                                        directionOf(viewerId, challenge),
                                        challenge.status(),
                                        TopicViews.toRef(challenge.topicId(), topic),
                                        toUserRef(challenge.challengerId(), profileById),
                                        toUserRef(challenge.challengedId(), profileById),
                                        challenge.createdAt(),
                                        challenge.expiresAt(),
                                        challenge.gameId(),
                                        challenge.replayGameId(),
                                        myRunGameId,
                                        opponentRunGameId,
                                        challenge.challengerScore(),
                                        challenge.challengedScore(),
                                        challenge.winnerId(),
                                        challenge.completedAt(),
                                        actionsFor(viewerId, challenge));
                            });
                });
    }

    private CompletableFuture<List<ChallengeCardView>> enrich(String viewerId, List<Challenge> challenges) {
        List<String> topicIds = challenges.stream().map(Challenge::topicId).distinct().toList();
        List<String> opponentIds = challenges.stream()
                .map(challenge -> opponentIdOf(viewerId, challenge))
                .distinct()
                .toList();

        CompletableFuture<List<Topic>> topicsFuture = topicIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : queryGateway.query(new TopicQuery.GetTopicsByIdsQuery(topicIds),
                        QueryResponseTypes.multipleInstancesOf(Topic.class));
        CompletableFuture<List<Profile>> profilesFuture = opponentIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : profileLookup.getAll(opponentIds);

        return CompletableFuture.allOf(topicsFuture, profilesFuture)
                .thenApply(_ -> {
                    Map<String, Topic> topicById = topicsFuture.join().stream()
                            .collect(Collectors.toMap(Topic::topicId, Function.identity(),
                                    (first, _) -> first, LinkedHashMap::new));
                    Map<String, Profile> profileById = byId(profilesFuture.join());
                    return challenges.stream()
                            .map(challenge -> new ChallengeCardView(
                                    challenge.challengeId(),
                                    directionOf(viewerId, challenge),
                                    challenge.status(),
                                    TopicViews.toRef(challenge.topicId(), topicById.get(challenge.topicId())),
                                    toUserRef(opponentIdOf(viewerId, challenge), profileById),
                                    challenge.createdAt(),
                                    challenge.expiresAt(),
                                    challenge.gameId(),
                                    challenge.winnerId(),
                                    actionsFor(viewerId, challenge)))
                            .toList();
                });
    }

    private static List<ChallengeAction> actionsFor(String viewerId, Challenge challenge) {
        boolean isChallenged = viewerId.equals(challenge.challengedId());
        return switch (challenge.status()) {
            case PENDING -> isChallenged
                    ? List.of(ChallengeAction.ACCEPT, ChallengeAction.DECLINE)
                    : List.of(ChallengeAction.CANCEL);
            case ACCEPTED -> challenge.gameId() != null ? List.of(ChallengeAction.PLAY) : List.of();
            default -> List.of();
        };
    }

    private static ChallengeDirection directionOf(String viewerId, Challenge challenge) {
        return viewerId.equals(challenge.challengerId())
                ? ChallengeDirection.SENT
                : ChallengeDirection.RECEIVED;
    }

    private static String opponentIdOf(String viewerId, Challenge challenge) {
        return viewerId.equals(challenge.challengerId())
                ? challenge.challengedId()
                : challenge.challengerId();
    }

    private static UserRefView toUserRef(String userId, Map<String, Profile> profileById) {
        Profile profile = profileById.get(userId);
        return profile == null
                ? new UserRefView(userId, null, null)
                : new UserRefView(profile.userId(), profile.pseudonym(), profile.avatarOptions());
    }

    private static Map<String, Profile> byId(List<Profile> profiles) {
        return profiles.stream().collect(Collectors.toMap(Profile::userId, Function.identity(),
                (first, _) -> first, LinkedHashMap::new));
    }
}
