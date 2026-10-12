package io.github.quizup.bff.application;

import io.github.quizup.matchmaking.domain.command.RoomCommand;
import io.github.quizup.matchmaking.domain.model.Room;
import io.github.quizup.matchmaking.domain.model.RoomStatus;
import io.github.quizup.matchmaking.domain.query.RoomQuery;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie que la façade prépare le snapshot de profil dans la commande d'apparition (aucune I/O
 * de résolution des joueurs côté saga salle).
 */
class RoomViewServiceTest {

    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final ProgressionLookup progressionLookup = mock(ProgressionLookup.class);

    private final RoomViewService service =
            new RoomViewService(queryGateway, commandGateway, profileLookup, progressionLookup);

    @Test
    void join_preparesPlayerSnapshotInCommand() {
        stubRoom();
        when(profileLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(profile()));
        when(progressionLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(progress()));
        when(commandGateway.send(any(RoomCommand.JoinRoomCommand.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.join("room-1", "user-1").join();

        ArgumentCaptor<RoomCommand.JoinRoomCommand> captor =
                ArgumentCaptor.forClass(RoomCommand.JoinRoomCommand.class);
        verify(commandGateway).send(captor.capture());
        RoomCommand.JoinRoomCommand command = captor.getValue();
        assertThat(command.roomId()).isEqualTo("room-1");
        assertThat(command.playerId()).isEqualTo("user-1");
        assertThat(command.player().playerName()).isEqualTo("Alice");
        assertThat(command.player().language()).isEqualTo(Language.FR);
        assertThat(command.player().level()).isEqualTo(12);
        assertThat(command.player().xpTotal()).isEqualTo(250);
    }

    @Test
    void join_fallsBackToNeutralProgressionWhenMissing() {
        stubRoom();
        when(profileLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(profile()));
        when(progressionLookup.get("user-1"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("no progression")));
        when(commandGateway.send(any(RoomCommand.JoinRoomCommand.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.join("room-1", "user-1").join();

        ArgumentCaptor<RoomCommand.JoinRoomCommand> captor =
                ArgumentCaptor.forClass(RoomCommand.JoinRoomCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().player().level()).isEqualTo(1);
        assertThat(captor.getValue().player().xpTotal()).isZero();
    }

    private void stubRoom() {
        when(queryGateway.query(any(RoomQuery.GetRoomById.class), any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(Room.builder()
                        .roomId("room-1")
                        .topicId("topic-1")
                        .initiatorId("user-1")
                        .status(RoomStatus.WAITING_PLAYERS)
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .updatedAt(Instant.now())
                        .build()));
    }

    private static Profile profile() {
        return new Profile(
                "user-1",
                "user-1@quizup.test",
                "Alice",
                null,
                "FR",
                null,
                Language.FR,
                Instant.now(),
                Instant.now());
    }

    private static PlayerProgress progress() {
        return PlayerProgress.builder()
                .userId("user-1")
                .xpTotal(250)
                .level(12)
                .xpByTopic(Map.of())
                .badges(Set.of())
                .gamesPlayed(10)
                .wins(6)
                .losses(3)
                .draws(1)
                .bestScore(900)
                .currentWinStreak(2)
                .bestWinStreak(4)
                .updatedAt(Instant.now())
                .build();
    }
}
