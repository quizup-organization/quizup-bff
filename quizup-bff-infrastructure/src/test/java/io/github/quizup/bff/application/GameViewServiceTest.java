package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameResult;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;
import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Composition du résultat de duel : récompense XP du duel et progression <b>à l'instant de la
 * partie</b> (snapshot porté par l'agrégat game + XP gagnée sur ce duel).
 */
class GameViewServiceTest {

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final ProgressionLookup progressionLookup = mock(ProgressionLookup.class);
    private final IdentityLookup identityLookup = mock(IdentityLookup.class);
    private final GameViewService service =
            new GameViewService(commandGateway, queryGateway, profileLookup, progressionLookup, identityLookup);

    @Test
    void result_derives_victory_bonus_from_game_xp() {
        stubResult(gameResult(120, 90), List.of(new GameXp("game-1", 150, 18, true)));

        GameResultView view = service.result("game-1", "player-1").join();

        assertThat(view.myScore()).isEqualTo(120);
        assertThat(view.opponentScore()).isEqualTo(90);
        assertThat(view.reward()).isEqualTo(new GameResultView.RewardView(150, 30));
        assertThat(view.opponentLevel()).isEqualTo(3);
        assertThat(view.opponentTitle()).isEqualTo(ProgressionRules.titleFor(3));
    }

    @Test
    void result_builds_progression_from_game_snapshot_plus_gained_xp() {
        stubResult(gameResult(120, 90), List.of(new GameXp("game-1", 150, 18, true)));

        GameResultView view = service.result("game-1", "player-1").join();

        // Snapshot à la création (100) + XP gagnée sur ce duel (150).
        int xpTotal = 250;
        int level = ProgressionRules.levelFor(xpTotal);
        assertThat(view.progression().xpTotal()).isEqualTo(xpTotal);
        assertThat(view.progression().level()).isEqualTo(level);
        assertThat(view.progression().title()).isEqualTo(ProgressionRules.titleFor(level));
        assertThat(view.progression().xpForNextLevel()).isEqualTo(ProgressionRules.xpForNextLevel(level));
        assertThat(view.progression().levelProgressPercent())
                .isEqualTo(ProgressionViews.levelProgressPercent(xpTotal, level));
    }

    @Test
    void result_has_no_reward_when_game_xp_is_missing() {
        stubResult(gameResult(120, 90), List.of());

        GameResultView view = service.result("game-1", "player-1").join();

        assertThat(view.reward()).isNull();
        assertThat(view.progression().xpTotal()).isEqualTo(100);
    }

    @Test
    void result_training_bot_game_has_zero_reward_without_negative_victory_bonus() {
        stubResult(gameResult(120, 90, true), List.of(new GameXp("game-1", 0, 0, false)));

        GameResultView view = service.result("game-1", "player-1").join();

        assertThat(view.botGame()).isTrue();
        assertThat(view.reward()).isEqualTo(new GameResultView.RewardView(0, 0));
    }

    @Test
    void result_rejects_a_non_participant() {
        GameResult result = gameResult(120, 90);
        when(queryGateway.query(
                any(GameQuery.GetGameByIdQuery.class),
                ArgumentMatchers.<ResponseType<Game>>any()))
                .thenReturn(CompletableFuture.completedFuture(game(result)));

        assertThatThrownBy(() -> service.result("game-1", "intruder").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BffProblems.GameAccessDeniedProblem.class);
    }

    @Test
    void events_are_reserved_to_participants() {
        GameResult result = gameResult(120, 90);
        when(queryGateway.query(
                any(GameQuery.GetGameByIdQuery.class),
                ArgumentMatchers.<ResponseType<Game>>any()))
                .thenReturn(CompletableFuture.completedFuture(game(result)));
        when(queryGateway.query(
                any(GameQuery.GetGameEventsQuery.class),
                ArgumentMatchers.<ResponseType<List<EventEnvelope>>>any()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        assertThat(service.events("game-1", "player-1").join()).isEmpty();
        assertThatThrownBy(() -> service.events("game-1", "intruder").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BffProblems.GameAccessDeniedProblem.class);
    }

    private void stubResult(GameResult result, List<GameXp> xp) {
        when(queryGateway.query(
                any(GameQuery.GetGameByIdQuery.class),
                ArgumentMatchers.<ResponseType<Game>>any()))
                .thenReturn(CompletableFuture.completedFuture(game(result)));
        when(queryGateway.query(
                any(GameQuery.GetGameResultQuery.class),
                ArgumentMatchers.<ResponseType<GameResult>>any()))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(queryGateway.query(
                any(ProgressionQuery.GetGamesXpQuery.class),
                ArgumentMatchers.<ResponseType<List<GameXp>>>any()))
                .thenReturn(CompletableFuture.completedFuture(xp));
        when(queryGateway.query(
                any(TopicQuery.GetTopicsByIdsQuery.class),
                ArgumentMatchers.<ResponseType<List<Topic>>>any()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(profileLookup.get(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(progressionLookup.get(any())).thenAnswer(invocation ->
                CompletableFuture.completedFuture(PlayerProgress.empty(invocation.getArgument(0))));
        when(identityLookup.status(any())).thenReturn(CompletableFuture.completedFuture(
                new io.github.quizup.identity.domain.model.UserAccountStatus(
                        "player-2", io.github.quizup.identity.domain.model.AccountStatus.ACTIVE, null)));
    }

    private static Game game(GameResult result) {
        return Game.builder()
                .gameId(result.gameId())
                .topicId(result.topicId())
                .player1Id(result.player1Id())
                .player2Id(result.player2Id())
                .build();
    }

    private static GameResult gameResult(int myScore, int opponentScore) {
        return gameResult(myScore, opponentScore, false);
    }

    private static GameResult gameResult(int myScore, int opponentScore, boolean botGame) {
        return new GameResult(
                "game-1", "topic-1",
                "player-1", "Alice", "player-2", "Bob",
                "player-1", "player-2",
                myScore, opponentScore,
                myScore > opponentScore ? "player-1" : "player-2",
                botGame,
                null,
                100, 20, 6, 3, 7, 7,
                2, 100, 3, 400);
    }
}
