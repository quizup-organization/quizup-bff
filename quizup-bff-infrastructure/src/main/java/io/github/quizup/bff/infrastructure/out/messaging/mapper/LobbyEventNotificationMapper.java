package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.LobbyNotification;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Mappe les événements de salon privé vers le contrat web {@link LobbyNotification}.
 */
public final class LobbyEventNotificationMapper {

    private LobbyEventNotificationMapper() {
    }

    public static Optional<LobbyNotification> toNotification(LobbyEvent event) {
        if (isNull(event)) {
            return Optional.empty();
        }

        return switch (event) {
            case LobbyEvent.LobbyCreatedEvent e -> Optional.of(
                    new LobbyNotification.LobbyCreatedNotification(
                            e.lobbyId(), e.topicId(), e.initiatorId(), e.opponentId(), e.expiresAt()));
            case LobbyEvent.LobbyJoinedEvent e -> Optional.of(
                    new LobbyNotification.LobbyJoinedNotification(e.lobbyId(), e.participantId()));
            case LobbyEvent.LobbyDeclinedEvent e -> Optional.of(
                    new LobbyNotification.LobbyDeclinedNotification(e.lobbyId(), e.opponentId()));
            case LobbyEvent.LobbyCompletedEvent e -> Optional.of(
                    new LobbyNotification.LobbyCompletedNotification(e.lobbyId(), e.gameId()));
            case LobbyEvent.LobbyCancelledEvent e -> Optional.of(
                    new LobbyNotification.LobbyCancelledNotification(e.lobbyId(), e.reason()));
            case LobbyEvent.LobbyExpiredEvent e -> Optional.of(
                    new LobbyNotification.LobbyExpiredNotification(e.lobbyId()));
            case LobbyEvent.LobbyFailedEvent e -> Optional.of(
                    new LobbyNotification.LobbyFailedNotification(e.lobbyId(), e.reason()));
            case LobbyEvent.LobbyRoomEnteredEvent e -> Optional.of(
                    new LobbyNotification.LobbyRoomEnteredNotification(e.lobbyId(), e.playerId()));
            case LobbyEvent.LobbyLeftEvent e -> Optional.of(
                    new LobbyNotification.LobbyLeftNotification(e.lobbyId(), e.playerId()));
            case LobbyEvent.LobbyAllPlayersPresentEvent e -> Optional.of(
                    new LobbyNotification.LobbyAllPlayersPresentNotification(e.lobbyId(), e.readyDeadlineAt()));
            case LobbyEvent.LobbyMissedEvent e -> Optional.of(
                    new LobbyNotification.LobbyMissedNotification(e.lobbyId(), e.absentPlayerId(), e.reason()));
            default -> Optional.empty();
        };
    }
}
