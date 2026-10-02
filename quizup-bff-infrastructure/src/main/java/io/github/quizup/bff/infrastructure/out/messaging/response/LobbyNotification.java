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
        @JsonSubTypes.Type(value = LobbyNotification.LobbyCompletedNotification.class, name = "LOBBY_COMPLETED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyCancelledNotification.class, name = "LOBBY_CANCELLED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyExpiredNotification.class, name = "LOBBY_EXPIRED"),
        @JsonSubTypes.Type(value = LobbyNotification.LobbyFailedNotification.class, name = "LOBBY_FAILED")
})
public interface LobbyNotification {

    @JsonProperty("type")
    LobbyNotificationType type();

    String lobbyId();

    enum LobbyNotificationType {
        LOBBY_CREATED,
        LOBBY_JOINED,
        LOBBY_COMPLETED,
        LOBBY_CANCELLED,
        LOBBY_EXPIRED,
        LOBBY_FAILED
    }

    record LobbyCreatedNotification(
            String lobbyId,
            String topicId,
            String initiatorId,
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
}
