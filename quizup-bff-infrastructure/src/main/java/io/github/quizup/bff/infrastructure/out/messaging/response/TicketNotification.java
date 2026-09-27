package io.github.quizup.bff.infrastructure.out.messaging.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Notifications de ticket de matchmaking — contrat web du BFF. {@code LobbyJoinedEvent} n'est pas
 * exposé : le ticket ne passe à {@code MATCHED} qu'avec une partie créée.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TicketNotification.SearchingTicketNotification.class, name = "SEARCHING"),
        @JsonSubTypes.Type(value = TicketNotification.MatchedTicketNotification.class, name = "MATCHED"),
        @JsonSubTypes.Type(value = TicketNotification.CancelledTicketNotification.class, name = "CANCELLED")
})
public interface TicketNotification {

    @JsonProperty("type")
    TicketNotificationType type();

    String ticketId();

    enum TicketNotificationType {
        SEARCHING,
        MATCHED,
        CANCELLED
    }

    record SearchingTicketNotification(
            String ticketId,
            String topicId
    ) implements TicketNotification {
        @Override
        public TicketNotificationType type() {
            return TicketNotificationType.SEARCHING;
        }
    }

    record MatchedTicketNotification(
            String ticketId,
            String gameId,
            String initiatorId,
            String challengerId,
            boolean vsBot
    ) implements TicketNotification {
        @Override
        public TicketNotificationType type() {
            return TicketNotificationType.MATCHED;
        }
    }

    record CancelledTicketNotification(
            String ticketId
    ) implements TicketNotification {
        @Override
        public TicketNotificationType type() {
            return TicketNotificationType.CANCELLED;
        }
    }
}
