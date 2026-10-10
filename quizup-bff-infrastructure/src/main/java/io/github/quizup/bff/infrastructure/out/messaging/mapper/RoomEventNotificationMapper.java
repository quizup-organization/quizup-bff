package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.RoomNotification;
import io.github.quizup.matchmaking.domain.event.RoomEvent;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Mappe les événements de salle vers le contrat web {@link RoomNotification}.
 */
public final class RoomEventNotificationMapper {

    private RoomEventNotificationMapper() {
    }

    public static Optional<RoomNotification> toNotification(RoomEvent event) {
        if (isNull(event)) {
            return Optional.empty();
        }

        return switch (event) {
            case RoomEvent.RoomCreatedEvent e -> Optional.of(
                    new RoomNotification.RoomCreatedNotification(
                            e.roomId(), e.topicId(), e.initiatorId(), e.opponentId(), e.expiresAt()));
            case RoomEvent.RoomEnteredEvent e -> Optional.of(
                    new RoomNotification.RoomEnteredNotification(e.roomId(), e.playerId()));
            case RoomEvent.RoomAllPlayersPresentEvent e -> Optional.of(
                    new RoomNotification.RoomAllPlayersPresentNotification(e.roomId(), e.readyDeadlineAt()));
            case RoomEvent.RoomLeftEvent e -> Optional.of(
                    new RoomNotification.RoomLeftNotification(e.roomId(), e.playerId()));
            case RoomEvent.RoomCompletedEvent e -> Optional.of(
                    new RoomNotification.RoomCompletedNotification(e.roomId(), e.gameId()));
            case RoomEvent.RoomCancelledEvent e -> Optional.of(
                    new RoomNotification.RoomCancelledNotification(e.roomId(), e.reason()));
            case RoomEvent.RoomExpiredEvent e -> Optional.of(
                    new RoomNotification.RoomExpiredNotification(e.roomId()));
            case RoomEvent.RoomFailedEvent e -> Optional.of(
                    new RoomNotification.RoomFailedNotification(e.roomId(), e.reason()));
            default -> Optional.empty();
        };
    }
}
