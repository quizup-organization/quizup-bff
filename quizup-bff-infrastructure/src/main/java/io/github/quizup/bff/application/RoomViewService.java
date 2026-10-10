package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateRoomRequest;
import io.github.quizup.bff.infrastructure.in.api.response.RoomPhase;
import io.github.quizup.bff.infrastructure.in.api.response.RoomView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.matchmaking.domain.command.RoomCommand;
import io.github.quizup.matchmaking.domain.model.Room;
import io.github.quizup.matchmaking.domain.model.RoomStatus;
import io.github.quizup.matchmaking.domain.query.RoomQuery;
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
 * Salle (salle d'attente temps réel) : création, consultation, apparition, annulation.
 * Le lien de partage se compose côté client via {@code /join/{roomId}}.
 */
@Service
public class RoomViewService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public RoomViewService(QueryGateway queryGateway,
                           CommandGateway commandGateway,
                           ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    /**
     * Ouvre une salle partagée (lien). Le défi nominatif passe par {@code POST /api/challenges} :
     * la salle nominative n'est créée qu'à l'acceptation (saga).
     */
    public CompletableFuture<String> create(String playerId, CreateRoomRequest request) {
        String roomId = UUID.randomUUID().toString();
        return commandGateway
                .send(new RoomCommand.CreateRoomCommand(roomId, request.topicId(), playerId, null))
                .thenApply(_ -> roomId);
    }

    public CompletableFuture<RoomView> get(String roomId, String viewerId) {
        return queryGateway
                .query(new RoomQuery.GetRoomById(roomId), QueryResponseTypes.instanceOf(Room.class))
                .thenCompose(room -> enrich(List.of(room), viewerId).thenApply(views -> views.getFirst()));
    }

    public CompletableFuture<List<RoomView>> mine(String playerId) {
        return queryGateway
                .query(new RoomQuery.GetMyOpenRooms(playerId),
                        QueryResponseTypes.multipleInstancesOf(Room.class))
                .thenCompose(rooms -> enrich(rooms, playerId));
    }

    /**
     * Le joueur <b>apparaît</b> dans la salle : présence temps réel et, pour le second humain,
     * enregistrement comme participant. C'est la seule action d'entrée, émise par le client quand
     * l'utilisateur est réellement dans la salle.
     */
    public CompletableFuture<Void> join(String roomId, String playerId) {
        return requireRoom(roomId)
                .thenCompose(_ -> commandGateway
                        .send(new RoomCommand.JoinRoomCommand(roomId, playerId))
                        .thenAccept(_ -> {
                        }));
    }

    public CompletableFuture<Void> leave(String roomId, String playerId) {
        return requireRoom(roomId)
                .thenCompose(_ -> commandGateway
                        .send(new RoomCommand.LeaveRoomCommand(roomId, playerId))
                        .thenAccept(_ -> {
                        }));
    }

    public CompletableFuture<Void> cancel(String roomId, String playerId) {
        return requireRoom(roomId)
                .thenCompose(_ -> commandGateway
                        .send(new RoomCommand.CancelRoomCommand(roomId, playerId))
                        .thenAccept(_ -> {
                        }));
    }

    /**
     * Pré-vérification : une salle purgée (état terminal + rétention) doit répondre 404,
     * pas une {@code AggregateDeletedException} non mappée par le SDK.
     */
    private CompletableFuture<Void> requireRoom(String roomId) {
        return queryGateway
                .query(new RoomQuery.GetRoomById(roomId), QueryResponseTypes.instanceOf(Room.class))
                .thenAccept(_ -> {
                });
    }

    private CompletableFuture<List<RoomView>> enrich(List<Room> rooms, String viewerId) {
        List<String> topicIds = rooms.stream().map(Room::topicId).distinct().toList();
        List<String> opponentIds = rooms.stream()
                .map(room -> opponentIdOf(viewerId, room))
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

            return rooms.stream()
                    .map(room -> toView(room, viewerId, topicById, profileById))
                    .toList();
        });
    }

    private static RoomView toView(Room room,
                                   String viewerId,
                                   Map<String, Topic> topicById,
                                   Map<String, Profile> profileById) {
        String opponentId = opponentIdOf(viewerId, room);
        Profile opponent = opponentId == null ? null : profileById.get(opponentId);
        TopicRefView topic = TopicViews.toRef(room.topicId(), topicById.get(room.topicId()));
        UserRefView opponentRef = opponent == null
                ? null
                : new UserRefView(opponent.userId(), opponent.pseudonym(), opponent.avatarOptions());

        return new RoomView(
                room.roomId(),
                topic,
                room.status(),
                phaseOf(room),
                opponentRef,
                room.initiatorPresent(),
                room.participantPresent(),
                room.readyDeadlineAt(),
                room.gameId(),
                room.createdAt(),
                room.expiresAt(),
                room.updatedAt());
    }

    /** Phase affichable de la salle, dérivée du cycle de vie et des présences. */
    private static RoomPhase phaseOf(Room room) {
        if (room.status() == RoomStatus.FAILED) {
            return RoomPhase.FAILED;
        }
        if (room.gameId() != null) {
            return RoomPhase.COMPLETED;
        }
        if (room.status() == RoomStatus.CLOSED) {
            return RoomPhase.CLOSED;
        }
        if (room.allPresentAt() != null) {
            return RoomPhase.READY;
        }
        return room.participantId() == null
                ? RoomPhase.WAITING_PARTICIPANT
                : RoomPhase.WAITING_PRESENCE;
    }

    /**
     * L'adversaire du point de vue du viewer : le participant s'il est là, sinon l'invité du défi
     * nominatif (l'initiateur voit ainsi la cible avant son apparition en salle).
     */
    private static String opponentIdOf(String viewerId, Room room) {
        if (viewerId.equals(room.initiatorId())) {
            return room.participantId() != null ? room.participantId() : room.opponentId();
        }
        return room.initiatorId();
    }
}
