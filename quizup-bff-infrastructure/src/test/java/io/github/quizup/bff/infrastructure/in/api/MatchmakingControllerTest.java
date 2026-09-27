package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.MatchmakingTicketService;
import io.github.quizup.bff.infrastructure.in.api.response.MatchmakingTicketView;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchmakingControllerTest {

    private static final String USER_ID = "user-1";

    private final MatchmakingTicketService matchmakingTicketService = mock(MatchmakingTicketService.class);
    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MatchmakingController(matchmakingTicketService, queryGateway))
                .build();
        TestSecurity.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void enqueue_returns_created_with_ticket_location() throws Exception {
        MatchmakingTicketView view = ticket(MatchmakingTicketView.Status.SEARCHING, null);
        when(matchmakingTicketService.enqueue(USER_ID, "topic-1"))
                .thenReturn(CompletableFuture.completedFuture(view));

        MvcResult result = mockMvc.perform(post("/api/matchmaking/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"topic-1"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/matchmaking/tickets/ticket-1")))
                .andExpect(jsonPath("$.status").value("SEARCHING"));
    }

    @Test
    void ticket_returns_not_found_when_unknown() throws Exception {
        when(matchmakingTicketService.get("ticket-1", USER_ID))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        MvcResult result = mockMvc.perform(get("/api/matchmaking/tickets/ticket-1"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNotFound());
    }

    @Test
    void ticket_returns_matched_view() throws Exception {
        when(matchmakingTicketService.get("ticket-1", USER_ID))
                .thenReturn(CompletableFuture.completedFuture(
                        Optional.of(ticket(MatchmakingTicketView.Status.MATCHED, "game-1"))));

        MvcResult result = mockMvc.perform(get("/api/matchmaking/tickets/ticket-1"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.gameId").value("game-1"));
    }

    @Test
    void cancel_delegates_to_service() throws Exception {
        when(matchmakingTicketService.cancel(USER_ID, "ticket-1"))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(post("/api/matchmaking/tickets/ticket-1/cancel"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        verify(matchmakingTicketService).cancel(USER_ID, "ticket-1");
    }

    private static MatchmakingTicketView ticket(MatchmakingTicketView.Status status, String gameId) {
        Instant now = Instant.parse("2026-09-27T10:00:00Z");
        return new MatchmakingTicketView("ticket-1", "topic-1", status, now, now, gameId, null, false);
    }
}
