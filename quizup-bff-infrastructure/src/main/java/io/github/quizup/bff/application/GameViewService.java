package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ActiveGameView;
import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameResult;
import io.github.quizup.game.domain.model.PlayerProgressSnapshot;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Duel : écritures (création bot, réponse, abandon, annulation) et parties en cours du joueur
 * (reprise depuis l'accueil).
 */
@Service
public class GameViewService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;
    private final GameQuestionDrawer gameQuestionDrawer;

    public GameViewService(CommandGateway commandGateway,
                           QueryGateway queryGateway,
                           ProfileLookup profileLookup,
                           GameQuestionDrawer gameQuestionDrawer) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
        this.gameQuestionDrawer = gameQuestionDrawer;
    }

    /**
     * Parties en cours du joueur (bannière de reprise), les plus récentes d'abord. Collection
     * vide si aucune partie active : l'absence n'est pas une erreur (pas de 404).
     */
    public CompletableFuture<List<ActiveGameView>> activeGames(String userId) {
        return queryGateway
                .query(new GameQuery.GetActiveGamesQuery(userId),
                        QueryResponseTypes.multipleInstancesOf(Game.class))
                .thenCompose(games -> enrich(userId, games));
    }

    private CompletableFuture<List<ActiveGameView>> enrich(String userId, List<Game> games) {
        if (games.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<String> topicIds = games.stream().map(Game::topicId).distinct().toList();
        List<String> opponentIds = games.stream()
                .map(game -> opponentIdOf(userId, game))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        CompletableFuture<List<Topic>> topicsFuture = queryGateway.query(
                new TopicQuery.GetTopicsByIdsQuery(topicIds),
                QueryResponseTypes.multipleInstancesOf(Topic.class));
        CompletableFuture<List<Profile>> profilesFuture = opponentIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : profileLookup.getAll(opponentIds);

        return CompletableFuture.allOf(topicsFuture, profilesFuture).thenApply(_ -> {
            Map<String, Topic> topicById = topicsFuture.join().stream()
                    .collect(Collectors.toMap(Topic::topicId, Function.identity(), (first, _) -> first, LinkedHashMap::new));
            Map<String, Profile> profileById = profilesFuture.join().stream()
                    .collect(Collectors.toMap(Profile::userId, Function.identity(), (first, _) -> first, LinkedHashMap::new));

            return games.stream()
                    .map(game -> toActiveView(game, userId, topicById, profileById))
                    .toList();
        });
    }

    private static ActiveGameView toActiveView(Game game,
                                               String userId,
                                               Map<String, Topic> topicById,
                                               Map<String, Profile> profileById) {
        String opponentId = opponentIdOf(userId, game);
        Profile opponent = opponentId == null ? null : profileById.get(opponentId);
        TopicRefView topic = TopicViews.toRef(game.topicId(), topicById.get(game.topicId()));
        UserRefView opponentRef = opponent == null
                ? null
                : new UserRefView(opponent.userId(), opponent.pseudonym(), opponent.avatarOptions());
        return new ActiveGameView(
                game.gameId(), topic, opponentRef, game.opponent(), game.status(), game.createdAt());
    }

    private static String opponentIdOf(String userId, Game game) {
        if (GamePlayerType.BOT.equals(game.opponent())) {
            return null;
        }
        return userId.equals(game.player1Id()) ? game.player2Id() : game.player1Id();
    }

    public CompletableFuture<String> createBotGame(String playerId, CreateGameRequest request) {
        CompletableFuture<Profile> profileFuture = profileLookup.get(playerId);
        CompletableFuture<PlayerProgress> progressFuture = queryGateway.query(
                new ProgressionQuery.GetProgressionQuery(playerId),
                QueryResponseTypes.instanceOf(PlayerProgress.class));
        // Les questions sont tirées en parallèle du profil/progress : la commande part complète,
        // l'agrégat game ne fait plus aucun appel inter-service.
        CompletableFuture<List<GameQuestion>> questionsFuture = profileFuture.thenCompose(player ->
                gameQuestionDrawer.draw(request.topicId(), Set.of(player.language())));

        return CompletableFuture.allOf(profileFuture, progressFuture, questionsFuture)
                .thenApply(_ -> {
                    Profile player = profileFuture.join();
                    PlayerProgress progress = progressFuture.join();
                    BotDifficulty difficulty = BotDifficulty.fromOrDefault(request.difficulty());
                    return new GameCommand.CreateGameCommand(
                            UUID.randomUUID().toString(),
                            request.topicId(),
                            player.userId(),
                            player.pseudonym(),
                            QuizUpConstants.SYSTEM_USER_ID,
                            QuizUpConstants.SYSTEM_USER_NAME,
                            GamePlayerType.BOT,
                            difficulty,
                            new PlayerProgressSnapshot(progress.level(), progress.xpTotal()),
                            PlayerProgressSnapshot.forBot(difficulty),
                            questionsFuture.join());
                })
                .thenCompose(command -> commandGateway.send(command).thenApply(_ -> command.gameId()));
    }

    public CompletableFuture<Void> answer(String gameId, String playerId, GameQuestionChoice choice) {
        return commandGateway
                .send(new GameCommand.AnswerQuestionCommand(gameId, playerId, choice, Instant.now()))
                .thenAccept(_ -> {
                });
    }

    public CompletableFuture<Void> abandon(String gameId, String playerId) {
        return commandGateway
                .send(new GameCommand.ForfeitGameCommand(gameId, playerId))
                .thenAccept(_ -> {
                });
    }

    public CompletableFuture<Void> cancel(String gameId) {
        return commandGateway
                .send(new GameCommand.CancelGameCommand(gameId, "PLAYER_CANCELLED"))
                .thenAccept(_ -> {
                });
    }

    /**
     * Résultat détaillé d'une partie terminée pour le joueur courant, enrichi de la récompense
     * XP de cette partie ({@code null} tant que la projection n'est pas disponible) et de la
     * progression <b>à l'instant de la partie</b> : snapshot de progression porté par l'agrégat
     * game (niveau/XP à la création) + XP gagnée sur ce duel. La partie doit exister et le joueur
     * en faire partie, sinon le service game répond un {@code Problem} métier.
     */
    public CompletableFuture<GameResultView> result(String gameId, String playerId) {
        CompletableFuture<GameResult> resultFuture = queryGateway.query(
                new GameQuery.GetGameResultQuery(gameId, playerId),
                QueryResponseTypes.instanceOf(GameResult.class));
        CompletableFuture<List<GameXp>> xpFuture = queryGateway.query(
                new ProgressionQuery.GetGamesXpQuery(playerId, List.of(gameId)),
                QueryResponseTypes.multipleInstancesOf(GameXp.class));

        return resultFuture.thenCompose(gameResult -> {
            CompletableFuture<List<Topic>> topicsFuture = queryGateway.query(
                    new TopicQuery.GetTopicsByIdsQuery(List.of(gameResult.topicId())),
                    QueryResponseTypes.multipleInstancesOf(Topic.class));
            CompletableFuture<Profile> opponentFuture = gameResult.botGame() || gameResult.opponentId() == null
                    ? CompletableFuture.completedFuture(null)
                    : profileLookup.get(gameResult.opponentId());

            return CompletableFuture.allOf(topicsFuture, opponentFuture, xpFuture).thenApply(_ -> {
                GameXp gameXp = xpFuture.join().stream().findFirst().orElse(null);
                Integer xp = gameXp == null ? null : gameXp.xp();
                GameResultView.RewardView reward = xp == null
                        ? null
                        : new GameResultView.RewardView(xp, xp - gameResult.myScore());

                int xpTotal = gameResult.myXpTotal() + (xp == null ? 0 : xp);
                int level = ProgressionRules.levelFor(xpTotal);
                GameResultView.ProgressionResultView progression = new GameResultView.ProgressionResultView(
                        xpTotal,
                        level,
                        ProgressionRules.titleFor(level),
                        ProgressionRules.xpForNextLevel(level),
                        ProgressionViews.levelProgressPercent(xpTotal, level));

                Topic topic = topicsFuture.join().stream().findFirst().orElse(null);
                Profile opponent = opponentFuture.join();
                UserRefView opponentRef = opponent == null
                        ? null
                        : new UserRefView(opponent.userId(), opponent.pseudonym(), opponent.avatarOptions());

                return new GameResultView(
                        gameResult.myScore(),
                        gameResult.opponentScore(),
                        gameResult.winnerId(),
                        gameResult.botGame(),
                        gameResult.basePoints(),
                        gameResult.speedBonus(),
                        gameResult.correctAnswers(),
                        gameResult.fastAnswers(),
                        gameResult.answeredRounds(),
                        gameResult.totalRounds(),
                        reward,
                        progression,
                        gameResult.opponentLevel(),
                        ProgressionRules.titleFor(gameResult.opponentLevel()),
                        TopicViews.toRef(gameResult.topicId(), topic),
                        opponentRef,
                        gameResult.opponentId(),
                        gameResult.botDifficulty());
            });
        });
    }
}
