package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameResult;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Composition du résultat de duel (récompense XP + progression) et revanches (union des langues
 * des deux joueurs).
 */
class GameViewServiceTest {

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final GameViewService service = new GameViewService(commandGateway, queryGateway, profileLookup);

    @Test
    void result_derives_victory_bonus_from_game_xp() {
        stubResult(gameResult(120, 90), List.of(new GameXp("game-1", 150)), PlayerProgress.empty("player-1")
                .toBuilder()
                .xpTotal(250)
                .build());

        GameResultView view = service.result("game-1", "player-1").join();

        assertThat(view.myScore()).isEqualTo(120);
        assertThat(view.opponentScore()).isEqualTo(90);
        assertThat(view.reward()).isEqualTo(new GameResultView.RewardView(150, 30));
    }

    @Test
    void result_reuses_progression_rules_for_level_and_palier() {
        stubResult(gameResult(120, 90), List.of(new GameXp("game-1", 150)), PlayerProgress.empty("player-1")
                .toBuilder()
                .xpTotal(250)
                .build());

        GameResultView view = service.result("game-1", "player-1").join();

        int level = ProgressionRules.levelFor(250);
        assertThat(view.progression().xpTotal()).isEqualTo(250);
        assertThat(view.progression().level()).isEqualTo(level);
        assertThat(view.progression().title()).isEqualTo(ProgressionRules.titleFor(level));
        assertThat(view.progression().xpForNextLevel()).isEqualTo(ProgressionRules.xpForNextLevel(level));
        assertThat(view.progression().levelProgressPercent())
                .isEqualTo(ProgressionViews.levelProgressPercent(250, level));
    }

    @Test
    void result_has_no_reward_when_game_xp_is_missing() {
        stubResult(gameResult(120, 90), List.of(), PlayerProgress.empty("player-1"));

        GameResultView view = service.result("game-1", "player-1").join();

        assertThat(view.reward()).isNull();
        assertThat(view.progression()).isNotNull();
    }

    @Test
    void request_rematch_sends_union_of_both_players_languages() {
        stubProfiles(Language.FR, Language.EN);
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.requestRematch("game-1", "player-1").join();

        ArgumentCaptor<GameCommand.RequestRematchCommand> captor =
                ArgumentCaptor.forClass(GameCommand.RequestRematchCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().gameId()).isEqualTo("game-1");
        assertThat(captor.getValue().playerId()).isEqualTo("player-1");
        assertThat(captor.getValue().languages()).containsExactlyInAnyOrder(Language.FR, Language.EN);
    }

    @Test
    void request_rematch_collapses_shared_language_to_single_entry() {
        stubProfiles(Language.FR, Language.FR);
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.requestRematch("game-1", "player-1").join();

        ArgumentCaptor<GameCommand.RequestRematchCommand> captor =
                ArgumentCaptor.forClass(GameCommand.RequestRematchCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().languages()).containsExactly(Language.FR);
    }

    @Test
    void request_rematch_against_bot_uses_requester_language_only() {
        when(queryGateway.query(
                any(GameQuery.GetGameByIdQuery.class),
                ArgumentMatchers.<ResponseType<Game>>any()))
                .thenReturn(CompletableFuture.completedFuture(botGame()));
        when(profileLookup.get("player-1")).thenReturn(CompletableFuture.completedFuture(
                Profile.builder().userId("player-1").language(Language.FR).build()));
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.requestRematch("game-1", "player-1").join();

        ArgumentCaptor<GameCommand.RequestRematchCommand> captor =
                ArgumentCaptor.forClass(GameCommand.RequestRematchCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().languages()).containsExactly(Language.FR);
    }

    @Test
    void rematch_responses_send_player_commands() {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.acceptRematch("game-1", "player-2").join();
        service.declineRematch("game-1", "player-2").join();
        service.cancelRematch("game-1", "player-2").join();

        verify(commandGateway).send(new GameCommand.AcceptRematchCommand("game-1", "player-2"));
        verify(commandGateway).send(new GameCommand.DeclineRematchCommand("game-1", "player-2"));
        verify(commandGateway).send(new GameCommand.CancelRematchCommand("game-1", "player-2"));
    }

    private void stubResult(GameResult result, List<GameXp> xp, PlayerProgress progress) {
        when(queryGateway.query(
                any(GameQuery.GetGameResultQuery.class),
                ArgumentMatchers.<ResponseType<GameResult>>any()))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(queryGateway.query(
                any(ProgressionQuery.GetGamesXpQuery.class),
                ArgumentMatchers.<ResponseType<List<GameXp>>>any()))
                .thenReturn(CompletableFuture.completedFuture(xp));
        when(queryGateway.query(
                any(ProgressionQuery.GetProgressionQuery.class),
                ArgumentMatchers.<ResponseType<PlayerProgress>>any()))
                .thenReturn(CompletableFuture.completedFuture(progress));
    }

    private void stubProfiles(Language requester, Language opponent) {
        when(queryGateway.query(
                any(GameQuery.GetGameByIdQuery.class),
                ArgumentMatchers.<ResponseType<Game>>any()))
                .thenReturn(CompletableFuture.completedFuture(humanGame()));
        when(profileLookup.get("player-1")).thenReturn(CompletableFuture.completedFuture(
                Profile.builder().userId("player-1").language(requester).build()));
        when(profileLookup.get("player-2")).thenReturn(CompletableFuture.completedFuture(
                Profile.builder().userId("player-2").language(opponent).build()));
    }

    private static GameResult gameResult(int myScore, int opponentScore) {
        return new GameResult(
                "game-1", "topic-1",
                "player-1", "Alice", "player-2", "Bob",
                "player-1", "player-2",
                myScore, opponentScore,
                myScore > opponentScore ? "player-1" : "player-2",
                false,
                100, 20, 6, 3, 7, 7);
    }

    private static Game humanGame() {
        return game(GamePlayerType.HUMAN);
    }

    private static Game botGame() {
        return game(GamePlayerType.BOT);
    }

    private static Game game(GamePlayerType opponent) {
        return Game.builder()
                .gameId("game-1")
                .topicId("topic-1")
                .player1Id("player-1")
                .player1Name("Alice")
                .player2Id("player-2")
                .player2Name("Bob")
                .opponent(opponent)
                .build();
    }
}
