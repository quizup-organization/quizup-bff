package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.GameViewService;
import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.in.api.response.CurrentGameView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameStatus;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest {

    private static final String USER_ID = "user-1";

    private final GameViewService gameViewService = mock(GameViewService.class);
    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameViewService, queryGateway)).build();
        TestSecurity.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void create_bot_game_returns_created_with_location() throws Exception {
        when(gameViewService.createBotGame(org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(CreateGameRequest.class)))
                .thenReturn(CompletableFuture.completedFuture("game-1"));

        MvcResult result = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"topic-1"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/games/game-1")));
    }

    @Test
    void answer_sends_choice_without_player_in_body() throws Exception {
        when(gameViewService.answer("game-1", USER_ID, GameQuestionChoice.A))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(post("/api/games/game-1/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choice":"A"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        verify(gameViewService).answer("game-1", USER_ID, GameQuestionChoice.A);
    }

    @Test
    void abandon_is_always_accepted() throws Exception {
        when(gameViewService.abandon("game-1", USER_ID))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(post("/api/games/game-1/abandon"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        verify(gameViewService).abandon("game-1", USER_ID);
    }

    @Test
    void current_returns_the_resumable_game() throws Exception {
        CurrentGameView view = new CurrentGameView(
                "game-1",
                new TopicRefView("topic-1", "Culture générale", "GENERAL", "🌍", "#ffffff", null),
                new UserRefView("opponent-1", "Bob", null),
                GamePlayerType.HUMAN,
                GameStatus.IN_PROGRESS,
                Instant.parse("2026-10-05T10:00:00Z"));
        when(gameViewService.current(USER_ID)).thenReturn(CompletableFuture.completedFuture(view));

        MvcResult result = mockMvc.perform(get("/api/games/current"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        verify(gameViewService).current(USER_ID);
    }

    @Test
    void current_returns_no_content_without_game() throws Exception {
        when(gameViewService.current(USER_ID)).thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(get("/api/games/current"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(gameViewService).current(USER_ID);
    }

    @Test
    void join_and_leave_delegate_to_service() throws Exception {
        when(gameViewService.join("game-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(null));
        when(gameViewService.leave("game-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(null));

        MvcResult joinResult = mockMvc.perform(post("/api/games/game-1/join"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(joinResult)).andExpect(status().isOk());
        verify(gameViewService).join("game-1", USER_ID);

        MvcResult leaveResult = mockMvc.perform(post("/api/games/game-1/leave"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(leaveResult)).andExpect(status().isOk());
        verify(gameViewService).leave("game-1", USER_ID);
    }
}
