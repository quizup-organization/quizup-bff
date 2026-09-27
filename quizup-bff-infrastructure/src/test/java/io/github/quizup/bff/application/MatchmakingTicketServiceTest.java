package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.MatchmakingTicketView;
import io.github.quizup.matchmaking.domain.model.MatchmakingTicket;
import io.github.quizup.matchmaking.domain.model.MatchmakingTicketStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchmakingTicketServiceTest {

    private static final Instant CREATED = Instant.parse("2026-09-27T10:00:00Z");

    @Test
    void searching_ticket_maps_to_searching() {
        MatchmakingTicket ticket = ticket(MatchmakingTicketStatus.SEARCHING, null, null, false);

        MatchmakingTicketView view = MatchmakingTicketService.toView(ticket, "initiator");

        assertEquals(MatchmakingTicketView.Status.SEARCHING, view.status());
        assertNull(view.gameId());
        assertNull(view.opponentId());
    }

    @Test
    void matched_ticket_maps_opponent_relative_to_requester() {
        MatchmakingTicket ticket = ticket(MatchmakingTicketStatus.MATCHED, "challenger", "game-1", false);

        MatchmakingTicketView asInitiator = MatchmakingTicketService.toView(ticket, "initiator");
        assertEquals(MatchmakingTicketView.Status.MATCHED, asInitiator.status());
        assertEquals("game-1", asInitiator.gameId());
        assertEquals("challenger", asInitiator.opponentId());

        MatchmakingTicketView asChallenger = MatchmakingTicketService.toView(ticket, "challenger");
        assertEquals("initiator", asChallenger.opponentId());
    }

    @Test
    void cancelled_ticket_maps_to_cancelled() {
        MatchmakingTicketView view = MatchmakingTicketService.toView(
                ticket(MatchmakingTicketStatus.CANCELLED, "challenger", "game-1", false), "initiator");

        assertEquals(MatchmakingTicketView.Status.CANCELLED, view.status());
        assertNull(view.opponentId());
    }

    private static MatchmakingTicket ticket(MatchmakingTicketStatus status,
                                            String opponentId,
                                            String gameId,
                                            boolean vsBot) {
        return MatchmakingTicket.builder()
                .ticketId("ticket-1")
                .topicId("topic-1")
                .initiatorId("initiator")
                .opponentId(opponentId)
                .gameId(gameId)
                .vsBot(vsBot)
                .status(status)
                .createdAt(CREATED)
                .updatedAt(CREATED)
                .build();
    }
}
