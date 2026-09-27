package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ChallengeViewService;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeAction;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeCardView;
import io.github.quizup.bff.infrastructure.in.api.response.ChallengeDirection;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PendingCountView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.social.domain.command.ChallengeCommand;
import io.github.quizup.social.domain.model.ChallengeBox;
import io.github.quizup.social.domain.model.ChallengeStatus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

class ChallengeControllerTest {

    private static final String USER_ID = "user-1";

    private final ChallengeViewService challengeViewService = mock(ChallengeViewService.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChallengeController(challengeViewService, commandGateway))
                .build();
        TestSecurity.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void list_returns_enriched_cards() throws Exception {
        ChallengeCardView card = new ChallengeCardView(
                "challenge-1",
                ChallengeDirection.RECEIVED,
                ChallengeStatus.PENDING,
                new TopicRefView("topic-1", "Pokémon", "GAMES", null, null),
                new UserRefView("user-2", "Bravo", null),
                Instant.parse("2026-09-27T10:00:00Z"),
                Instant.parse("2026-09-28T10:00:00Z"),
                null,
                null,
                List.of(ChallengeAction.ACCEPT, ChallengeAction.DECLINE));
        when(challengeViewService.list(eq(USER_ID), eq(ChallengeBox.RECEIVED), isNull(), eq(0), eq(20)))
                .thenReturn(CompletableFuture.completedFuture(PageResponse.of(List.of(card), 0, 20, 1)));

        MvcResult result = mockMvc.perform(get("/api/challenges").param("box", "RECEIVED"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].challengeId").value("challenge-1"))
                .andExpect(jsonPath("$.content[0].actions[0]").value("ACCEPT"));
    }

    @Test
    void pending_count_returns_count() throws Exception {
        when(challengeViewService.pendingCount(USER_ID))
                .thenReturn(CompletableFuture.completedFuture(new PendingCountView(3)));

        MvcResult result = mockMvc.perform(get("/api/challenges/pending-count"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void create_returns_created_with_location() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture("challenge-1"));

        MvcResult result = mockMvc.perform(post("/api/challenges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"challengedId":"user-2","topicId":"topic-1"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void accept_sends_player_command() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture("challenge-1"));

        MvcResult result = mockMvc.perform(post("/api/challenges/challenge-1/accept"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        verify(commandGateway).send(new ChallengeCommand.AcceptChallengeCommand("challenge-1", USER_ID));
    }
}
