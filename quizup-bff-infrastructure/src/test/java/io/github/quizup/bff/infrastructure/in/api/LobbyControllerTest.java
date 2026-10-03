package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.LobbyViewService;
import io.github.quizup.bff.infrastructure.in.api.request.CreateLobbyRequest;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LobbyControllerTest {

    private static final String USER_ID = "user-1";

    private final LobbyViewService lobbyViewService = mock(LobbyViewService.class);
    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LobbyController(lobbyViewService, queryGateway)).build();
        TestSecurity.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void create_lobby_returns_created_with_location() throws Exception {
        when(lobbyViewService.create(eq(USER_ID), any(CreateLobbyRequest.class)))
                .thenReturn(CompletableFuture.completedFuture("lobby-1"));

        MvcResult result = mockMvc.perform(post("/api/lobbies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"topic-1"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/lobbies/lobby-1")));
    }

    @Test
    void join_leave_cancel_decline_delegate_to_service() throws Exception {
        when(lobbyViewService.join("lobby-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(null));
        when(lobbyViewService.leave("lobby-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(null));
        when(lobbyViewService.cancel("lobby-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(null));
        when(lobbyViewService.decline("lobby-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(null));

        MvcResult join = mockMvc.perform(post("/api/lobbies/lobby-1/join"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(join)).andExpect(status().isOk());

        MvcResult leave = mockMvc.perform(post("/api/lobbies/lobby-1/leave"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(leave)).andExpect(status().isOk());

        MvcResult cancel = mockMvc.perform(post("/api/lobbies/lobby-1/cancel"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(cancel)).andExpect(status().isOk());

        MvcResult decline = mockMvc.perform(post("/api/lobbies/lobby-1/decline"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(decline)).andExpect(status().isOk());

        verify(lobbyViewService).join("lobby-1", USER_ID);
        verify(lobbyViewService).leave("lobby-1", USER_ID);
        verify(lobbyViewService).cancel("lobby-1", USER_ID);
        verify(lobbyViewService).decline("lobby-1", USER_ID);
    }
}
