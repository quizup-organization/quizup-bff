package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateChallengeRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.matchmaking.domain.command.ChallengeCommand;
import io.github.quizup.matchmaking.domain.model.Challenge;
import io.github.quizup.matchmaking.domain.query.ChallengeQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Défi nominatif (intention asynchrone) : création, consultation, acceptation/refus/annulation.
 * La salle temps réel naît à l'acceptation (saga matchmaking).
 */
@Service
public class ChallengeViewService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public ChallengeViewService(QueryGateway queryGateway,
                                CommandGateway commandGateway,
                                ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    public CompletableFuture<String> create(String challengerId, CreateChallengeRequest request) {
        String challengeId = UUID.randomUUID().toString();
        // L'adversaire doit exister (404 sinon, jamais de défi orphelin).
        return profileLookup.get(request.opponentId())
                .thenCompose(_ -> commandGateway.send(new ChallengeCommand.CreateChallengeCommand(
                        challengeId, request.topicId(), challengerId, request.opponentId())))
                .thenApply(_ -> challengeId);
    }

    public CompletableFuture<ChallengeView> get(String challengeId) {
        return queryGateway
                .query(new ChallengeQuery.GetChallengeById(challengeId),
                        QueryResponseTypes.instanceOf(Challenge.class))
                .thenCompose(challenge -> enrich(List.of(challenge))
                        .thenApply(views -> views.getFirst()));
    }

    public CompletableFuture<List<ChallengeView>> mine(String playerId) {
        return queryGateway
                .query(new ChallengeQuery.GetMyChallenges(playerId),
                        QueryResponseTypes.multipleInstancesOf(Challenge.class))
                .thenCompose(this::enrich);
    }

    public CompletableFuture<Void> accept(String challengeId, String playerId) {
        return requireChallenge(challengeId)
                .thenCompose(_ -> commandGateway
                        .send(new ChallengeCommand.AcceptChallengeCommand(challengeId, playerId))
                        .thenAccept(_ -> {
                        }));
    }

    public CompletableFuture<Void> decline(String challengeId, String playerId) {
        return requireChallenge(challengeId)
                .thenCompose(_ -> commandGateway
                        .send(new ChallengeCommand.DeclineChallengeCommand(challengeId, playerId))
                        .thenAccept(_ -> {
                        }));
    }

    public CompletableFuture<Void> cancel(String challengeId, String playerId) {
        return requireChallenge(challengeId)
                .thenCompose(_ -> commandGateway
                        .send(new ChallengeCommand.CancelChallengeCommand(challengeId, playerId))
                        .thenAccept(_ -> {
                        }));
    }

    private CompletableFuture<Void> requireChallenge(String challengeId) {
        return queryGateway
                .query(new ChallengeQuery.GetChallengeById(challengeId),
                        QueryResponseTypes.instanceOf(Challenge.class))
                .thenAccept(_ -> {
                });
    }

    private CompletableFuture<List<ChallengeView>> enrich(List<Challenge> challenges) {
        List<String> topicIds = challenges.stream().map(Challenge::topicId).distinct().toList();
        List<String> playerIds = challenges.stream()
                .flatMap(challenge -> java.util.stream.Stream.of(
                        challenge.challengerId(), challenge.opponentId()))
                .distinct()
                .toList();

        CompletableFuture<List<Topic>> topicsFuture = topicIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : queryGateway.query(new TopicQuery.GetTopicsByIdsQuery(topicIds),
                        QueryResponseTypes.multipleInstancesOf(Topic.class));
        CompletableFuture<List<Profile>> profilesFuture = playerIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : profileLookup.getAll(playerIds);

        return CompletableFuture.allOf(topicsFuture, profilesFuture).thenApply(_ -> {
            Map<String, Topic> topicById = topicsFuture.join().stream()
                    .collect(Collectors.toMap(Topic::topicId, Function.identity(), (first, _) -> first, LinkedHashMap::new));
            Map<String, Profile> profileById = profilesFuture.join().stream()
                    .collect(Collectors.toMap(Profile::userId, Function.identity(), (first, _) -> first, LinkedHashMap::new));

            return challenges.stream()
                    .map(challenge -> toView(challenge, topicById, profileById))
                    .toList();
        });
    }

    private static ChallengeView toView(Challenge challenge,
                                        Map<String, Topic> topicById,
                                        Map<String, Profile> profileById) {
        TopicRefView topic = TopicViews.toRef(challenge.topicId(), topicById.get(challenge.topicId()));
        return new ChallengeView(
                challenge.challengeId(),
                topic,
                toRef(profileById.get(challenge.challengerId())),
                toRef(profileById.get(challenge.opponentId())),
                challenge.status(),
                challenge.roomId(),
                challenge.createdAt(),
                challenge.expiresAt());
    }

    private static UserRefView toRef(Profile profile) {
        return profile == null
                ? null
                : new UserRefView(profile.userId(), profile.pseudonym(), profile.avatarOptions());
    }
}
