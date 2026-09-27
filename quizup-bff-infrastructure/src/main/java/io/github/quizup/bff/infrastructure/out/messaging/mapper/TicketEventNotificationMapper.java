package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.TicketNotification;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import org.springframework.lang.Nullable;

/**
 * Traduit les événements internes du lobby vers le contrat web « ticket ».
 * {@code LobbyJoinedEvent} est une transition interne : le ticket ne devient {@code MATCHED}
 * qu'avec une partie créée ; {@code LobbyPurgedEvent} est un événement de rétention.
 */
public final class TicketEventNotificationMapper {

    private TicketEventNotificationMapper() {
    }

    @Nullable
    public static TicketNotification toNotification(LobbyEvent event) {
        if (event == null) {
            return null;
        }

        return switch (event) {
            case LobbyEvent.LobbyOpenedEvent opened -> new TicketNotification.SearchingTicketNotification(
                    opened.lobbyId(),
                    opened.topicId());

            case LobbyEvent.LobbyCompletedEvent completed -> new TicketNotification.MatchedTicketNotification(
                    completed.lobbyId(),
                    completed.gameId(),
                    completed.initiatorId(),
                    completed.challengerId(),
                    completed.vsBot());

            case LobbyEvent.LobbyCancelledEvent cancelled -> new TicketNotification.CancelledTicketNotification(
                    cancelled.lobbyId());

            default -> null;
        };
    }
}
