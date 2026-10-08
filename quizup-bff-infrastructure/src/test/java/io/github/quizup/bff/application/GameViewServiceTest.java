package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
import io.github.quizup.game.domain.model.GameResult;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
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
    private final GameViewService service = new GameViewService(commandGateway, queryGateway, profileLookup);

    @Test
    void result_derives_victory_bonus_from_game_xp() {
        stubResult(gameResult(120, 90), List.of(new GameXp("game-1", 150)));

        GameResultView view = service.result("game-1", "player-1").join();

        assertThat(view.myScore()).isEqualTo(120);
        assertThat(view.opponentScore()).isEqualTo(90);
        assertThat(view.reward()).isEqualTo(new GameResultView.RewardView(150, 30));
        assertThat(view.opponentLevel()).isEqualTo(3);
        assertThat(view.opponentTitle()).isEqualTo(ProgressionRules.titleFor(3));
    }

    @Test
    void result_builds_progression_from_game_snapshot_plus_gained_xp() {
        stubResult(gameResult(120, 90), List.of(new GameXp("game-1", 150)));

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

    private void stubResult(GameResult result, List<GameXp> xp) {
        when(queryGateway.query(
                any(GameQuery.GetGameResultQuery.class),
                ArgumentMatchers.<ResponseType<GameResult>>any()))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(queryGateway.query(
                any(ProgressionQuery.GetGamesXpQuery.class),
                ArgumentMatchers.<ResponseType<List<GameXp>>>any()))
                .thenReturn(CompletableFuture.completedFuture(xp));
    }

    private static GameResult gameResult(int myScore, int opponentScore) {
        return new GameResult(
                "game-1", "topic-1",
                "player-1", "Alice", "player-2", "Bob",
                "player-1", "player-2",
                myScore, opponentScore,
                myScore > opponentScore ? "player-1" : "player-2",
                false,
                100, 20, 6, 3, 7, 7,
                2, 100, 3, 400);
    }
}
