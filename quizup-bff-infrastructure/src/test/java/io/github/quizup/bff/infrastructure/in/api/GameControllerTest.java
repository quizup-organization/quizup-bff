package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.GameViewService;
import io.github.quizup.bff.infrastructure.in.api.problem.BffProblemExceptionHandler;
import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ActiveGameView;
import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
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
import java.util.List;
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

class GameControllerTest {

    private static final String USER_ID = "user-1";

    private final GameViewService gameViewService = mock(GameViewService.class);
    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameViewService, queryGateway))
                .setControllerAdvice(new BffProblemExceptionHandler())
                .build();
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
    void active_games_returns_the_collection() throws Exception {
        ActiveGameView view = new ActiveGameView(
                "game-1",
                new TopicRefView("topic-1", java.util.Map.of(io.github.quizup.microservice.core.domain.model.i18n.Language.FR, "Culture générale"), "GENERAL", "🌍", "#ffffff", null),
                new UserRefView("opponent-1", "Bob", null),
                GamePlayerType.HUMAN,
                GameStatus.IN_PROGRESS,
                Instant.parse("2026-10-05T10:00:00Z"));
        when(gameViewService.activeGames(USER_ID)).thenReturn(CompletableFuture.completedFuture(List.of(view)));

        MvcResult result = mockMvc.perform(get("/api/games").param("active", "true"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameId").value("game-1"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
        verify(gameViewService).activeGames(USER_ID);
    }

    @Test
    void active_games_returns_empty_collection_without_game() throws Exception {
        when(gameViewService.activeGames(USER_ID)).thenReturn(CompletableFuture.completedFuture(List.of()));

        MvcResult result = mockMvc.perform(get("/api/games").param("active", "true"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void games_without_active_filter_is_rejected() throws Exception {
        mockMvc.perform(get("/api/games"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:quizup:bff:game:invalidListRequest"));
    }

    @Test
    void result_returns_the_composed_game_result() throws Exception {
        GameResultView view = new GameResultView(
                120,
                90,
                USER_ID,
                false,
                100,
                20,
                6,
                3,
                7,
                7,
                new GameResultView.RewardView(150, 30),
                new GameResultView.ProgressionResultView(250, 2, "Apprenti", 400, 50),
                3,
                "Apprenti",
                new TopicRefView("topic-1", java.util.Map.of(io.github.quizup.microservice.core.domain.model.i18n.Language.FR, "Culture générale"), "GENERAL", "🌍", "#ffffff", null),
                new UserRefView("opponent-1", "Bob", null),
                "opponent-1",
                null);
        when(gameViewService.result("game-1", USER_ID)).thenReturn(CompletableFuture.completedFuture(view));

        MvcResult mvcResult = mockMvc.perform(get("/api/games/game-1/result"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myScore").value(120))
                .andExpect(jsonPath("$.reward.xp").value(150))
                .andExpect(jsonPath("$.progression.level").value(2))
                .andExpect(jsonPath("$.opponentLevel").value(3))
                .andExpect(jsonPath("$.opponentId").value("opponent-1"))
                .andExpect(jsonPath("$.topic.topicId").value("topic-1"));
        verify(gameViewService).result("game-1", USER_ID);
    }
}
