package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateLobbyRequest;
import io.github.quizup.bff.infrastructure.in.api.response.LobbyView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.matchmaking.domain.command.LobbyCommand;
import io.github.quizup.matchmaking.domain.model.Lobby;
import io.github.quizup.matchmaking.domain.query.LobbyQuery;
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
 * Salon privé (salle d'attente) : création, consultation, présence, annulation.
 * Le lien de partage se compose côté client via {@code /join/{lobbyId}}.
 */
@Service
public class LobbyViewService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public LobbyViewService(QueryGateway queryGateway,
                            CommandGateway commandGateway,
                            ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    public CompletableFuture<String> create(String playerId, CreateLobbyRequest request) {
        String lobbyId = UUID.randomUUID().toString();
        return commandGateway
                .send(new LobbyCommand.CreateLobbyCommand(lobbyId, request.topicId(), playerId))
                .thenApply(_ -> lobbyId);
    }

    public CompletableFuture<LobbyView> get(String lobbyId, String viewerId) {
        return queryGateway
                .query(new LobbyQuery.GetLobbyById(lobbyId), QueryResponseTypes.instanceOf(Lobby.class))
                .thenCompose(lobby -> enrich(List.of(lobby), viewerId).thenApply(views -> views.getFirst()));
    }

    public CompletableFuture<List<LobbyView>> mine(String playerId) {
        return queryGateway
                .query(new LobbyQuery.GetMyOpenLobbies(playerId),
                        QueryResponseTypes.multipleInstancesOf(Lobby.class))
                .thenCompose(lobbies -> enrich(lobbies, playerId));
    }

    public CompletableFuture<Void> join(String lobbyId, String playerId) {
        return commandGateway.send(new LobbyCommand.JoinLobbyCommand(lobbyId, playerId)).thenAccept(_ -> {
        });
    }

    public CompletableFuture<Void> leave(String lobbyId, String playerId) {
        return commandGateway.send(new LobbyCommand.LeaveLobbyCommand(lobbyId, playerId)).thenAccept(_ -> {
        });
    }

    public CompletableFuture<Void> cancel(String lobbyId, String playerId) {
        return commandGateway.send(new LobbyCommand.CancelLobbyCommand(lobbyId, playerId)).thenAccept(_ -> {
        });
    }

    private CompletableFuture<List<LobbyView>> enrich(List<Lobby> lobbies, String viewerId) {
        List<String> topicIds = lobbies.stream().map(Lobby::topicId).distinct().toList();
        List<String> opponentIds = lobbies.stream()
                .map(lobby -> opponentIdOf(viewerId, lobby))
                .filter(id -> id != null)
                .distinct()
                .toList();

        CompletableFuture<List<Topic>> topicsFuture = topicIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : queryGateway.query(new TopicQuery.GetTopicsByIdsQuery(topicIds),
                        QueryResponseTypes.multipleInstancesOf(Topic.class));
        CompletableFuture<List<Profile>> profilesFuture = opponentIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : profileLookup.getAll(opponentIds);

        return CompletableFuture.allOf(topicsFuture, profilesFuture).thenApply(_ -> {
            Map<String, Topic> topicById = topicsFuture.join().stream()
                    .collect(Collectors.toMap(Topic::topicId, Function.identity(), (first, _) -> first, LinkedHashMap::new));
            Map<String, Profile> profileById = profilesFuture.join().stream()
                    .collect(Collectors.toMap(Profile::userId, Function.identity(), (first, _) -> first, LinkedHashMap::new));

            return lobbies.stream()
                    .map(lobby -> toView(lobby, viewerId, topicById, profileById))
                    .toList();
        });
    }

    private static LobbyView toView(Lobby lobby,
                                    String viewerId,
                                    Map<String, Topic> topicById,
                                    Map<String, Profile> profileById) {
        String opponentId = opponentIdOf(viewerId, lobby);
        Profile opponent = opponentId == null ? null : profileById.get(opponentId);
        TopicRefView topic = TopicViews.toRef(lobby.topicId(), topicById.get(lobby.topicId()));
        UserRefView opponentRef = opponent == null
                ? null
                : new UserRefView(opponent.userId(), opponent.pseudonym(), opponent.avatarOptions());

        return new LobbyView(
                lobby.lobbyId(),
                topic,
                lobby.status(),
                opponentRef,
                lobby.gameId(),
                lobby.createdAt(),
                lobby.expiresAt(),
                lobby.updatedAt());
    }

    /** L'adversaire du point de vue du viewer : l'autre participant s'il existe. */
    private static String opponentIdOf(String viewerId, Lobby lobby) {
        if (viewerId.equals(lobby.initiatorId())) {
            return lobby.participantId();
        }
        return lobby.initiatorId();
    }
}
