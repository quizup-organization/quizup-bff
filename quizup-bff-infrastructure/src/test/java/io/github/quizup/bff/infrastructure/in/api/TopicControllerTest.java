package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.TopicViewService;
import io.github.quizup.bff.infrastructure.in.api.request.LeaderboardPeriod;
import io.github.quizup.bff.infrastructure.in.api.request.LeaderboardScope;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.TopicLeaderboardView;
import io.github.quizup.theme.domain.model.TopicSort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TopicControllerTest {

    private static final String USER_ID = "user-1";

    private final TopicViewService topicViewService = mock(TopicViewService.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TopicController(topicViewService, commandGateway)).build();
        TestSecurity.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void list_uses_default_sort_and_pagination() throws Exception {
        when(topicViewService.list(eq(USER_ID), isNull(), isNull(), eq(false), eq(TopicSort.POPULAR), eq(0), eq(24)))
                .thenReturn(CompletableFuture.completedFuture(PageResponse.of(List.of(), 0, 24, 0)));

        MvcResult result = mockMvc.perform(get("/api/topics"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void leaderboard_passes_requested_month() throws Exception {
        when(topicViewService.leaderboard(eq(USER_ID), eq("topic-1"), eq(LeaderboardPeriod.MONTHLY),
                eq("2026-08"), eq(LeaderboardScope.WORLD), eq(0), eq(25)))
                .thenReturn(CompletableFuture.completedFuture(
                        new TopicLeaderboardView(PageResponse.of(List.of(), 0, 25, 0), null)));

        MvcResult result = mockMvc.perform(get("/api/topics/topic-1/leaderboard")
                        .param("period", "MONTHLY")
                        .param("month", "2026-08"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.totalElements").value(0));

        verify(topicViewService).leaderboard(USER_ID, "topic-1", LeaderboardPeriod.MONTHLY,
                "2026-08", LeaderboardScope.WORLD, 0, 25);
    }

    @Test
    void follow_sends_deterministic_follow_id() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture("user-1:topic-1"));

        MvcResult result = mockMvc.perform(put("/api/topics/topic-1/follow"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new io.github.quizup.social.domain.command.TopicFollowerCommand.FollowTopicCommand(
                "user-1:topic-1", "topic-1", "user-1"));
    }
}
