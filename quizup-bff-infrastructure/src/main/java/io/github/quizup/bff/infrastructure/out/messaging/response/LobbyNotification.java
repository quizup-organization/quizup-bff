package io.github.quizup.bff.infrastructure.out.messaging.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

/**
 * Notifications de salon privé — contrat web du BFF (diffusées sur {@code /topic/lobbies/{lobbyId}}).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LobbyNotification.LobbyCreatedNotification.class, name = "LOBBY_CREATED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyJoinedNotification.class, name = "LOBBY_JOINED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyDeclinedNotification.class, name = "LOBBY_DECLINED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyCompletedNotification.class, name = "LOBBY_COMPLETED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyCancelledNotification.class, name = "LOBBY_CANCELLED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyExpiredNotification.class, name = "LOBBY_EXPIRED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyFailedNotification.class, name = "LOBBY_FAILED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyRoomEnteredNotification.class, name = "LOBBY_ROOM_ENTERED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyLeftNotification.class, name = "LOBBY_LEFT"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyAllPlayersPresentNotification.class, name = "LOBBY_ALL_PRESENT"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyMissedNotification.class, name = "LOBBY_MISSED")
})
public interface LobbyNotification {

    @JsonProperty("type")
    LobbyNotificationType type();

    String lobbyId();

    enum LobbyNotificationType {
        LOBBY_CREATED,
        LOBBY_JOINED,
        LOBBY_DECLINED,
        LOBBY_COMPLETED,
        LOBBY_CANCELLED,
        LOBBY_EXPIRED,
        LOBBY_FAILED,
        LOBBY_ROOM_ENTERED,
        LOBBY_LEFT,
        LOBBY_ALL_PRESENT,
        LOBBY_MISSED
    }

    record LobbyCreatedNotification(
            String lobbyId,
            String topicId,
            String initiatorId,
            String opponentId,
            Instant expiresAt
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_CREATED;
        }
    }

    record LobbyJoinedNotification(
            String lobbyId,
            String participantId
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_JOINED;
        }
    }

    record LobbyDeclinedNotification(
            String lobbyId,
            String opponentId
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_DECLINED;
        }
    }

    record LobbyCompletedNotification(
            String lobbyId,
            String gameId
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_COMPLETED;
        }
    }

    record LobbyCancelledNotification(
            String lobbyId,
            String reason
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_CANCELLED;
        }
    }

    record LobbyExpiredNotification(
            String lobbyId
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_EXPIRED;
        }
    }

    record LobbyFailedNotification(
            String lobbyId,
            String reason
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_FAILED;
        }
    }

    record LobbyRoomEnteredNotification(
            String lobbyId,
            String playerId
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_ROOM_ENTERED;
        }
    }

    record LobbyLeftNotification(
            String lobbyId,
            String playerId
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_LEFT;
        }
    }

    record LobbyAllPlayersPresentNotification(
            String lobbyId,
            Instant readyDeadlineAt
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_ALL_PRESENT;
        }
    }

    record LobbyMissedNotification(
            String lobbyId,
            String absentPlayerId,
            String reason
    ) implements LobbyNotification {
        @Override
        public LobbyNotificationType type() {
            return LobbyNotificationType.LOBBY_MISSED;
        }
    }
}
