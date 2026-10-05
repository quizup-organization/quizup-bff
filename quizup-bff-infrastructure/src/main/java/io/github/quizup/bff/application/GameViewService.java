package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.in.api.response.CurrentGameView;
import io.github.quizup.bff.infrastructure.in.api.response.GameResultView;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameResult;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Duel : écritures (création bot, entrée/sortie, réponse, abandon, annulation) et vue de
 * reprise de la partie en attente/en cours du joueur.
 */
@Service
public class GameViewService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;

    public GameViewService(CommandGateway commandGateway,
                           QueryGateway queryGateway,
                           ProfileLookup profileLookup) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
    }

    /**
     * Partie en attente/en cours du joueur (bannière de reprise). L'absence de partie
     * ({@code NoCurrentGameProblem}) n'est pas une erreur : la façade répond {@code 204}.
     */
    public CompletableFuture<CurrentGameView> current(String userId) {
        return queryGateway
                .query(new GameQuery.GetCurrentGameQuery(userId), QueryResponseTypes.instanceOf(Game.class))
                .exceptionally(error -> {
                    if (hasCause(error, GameExceptions.NoCurrentGameProblem.class)) {
                        return null;
                    }
                    throw new CompletionException(error);
                })
                .thenCompose(game -> {
                    if (game == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    String opponentId = opponentIdOf(userId, game);
                    CompletableFuture<Profile> opponentFuture = opponentId == null
                            ? CompletableFuture.completedFuture(null)
                            : profileLookup.get(opponentId);
                    CompletableFuture<List<Topic>> topicsFuture = queryGateway.query(
                            new TopicQuery.GetTopicsByIdsQuery(List.of(game.topicId())),
                            QueryResponseTypes.multipleInstancesOf(Topic.class));
                    return CompletableFuture.allOf(opponentFuture, topicsFuture).thenApply(_ -> {
                        Profile opponent = opponentFuture.join();
                        Topic topic = topicsFuture.join().stream().findFirst().orElse(null);
                        TopicRefView topicRef = TopicViews.toRef(game.topicId(), topic);
                        UserRefView opponentRef = opponent == null
                                ? null
                                : new UserRefView(opponent.userId(), opponent.pseudonym(), opponent.avatarOptions());
                        return new CurrentGameView(
                                game.gameId(), topicRef, opponentRef, game.opponent(),
                                game.status(), game.createdAt());
                    });
                });
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }

    private static String opponentIdOf(String userId, Game game) {
        if (GamePlayerType.BOT.equals(game.opponent())) {
            return null;
        }
        return userId.equals(game.player1Id()) ? game.player2Id() : game.player1Id();
    }

    public CompletableFuture<String> createBotGame(String playerId, CreateGameRequest request) {
        return profileLookup.get(playerId).thenCompose(player -> {
            String gameId = UUID.randomUUID().toString();
            GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                    gameId,
                    request.topicId(),
                    player.userId(),
                    player.pseudonym(),
                    QuizUpConstants.SYSTEM_USER_ID,
                    QuizUpConstants.SYSTEM_USER_NAME,
                    Set.of(player.language()),
                    GamePlayerType.BOT,
                    BotDifficulty.fromOrDefault(request.difficulty()));
            return commandGateway.send(command).thenApply(_ -> gameId);
        });
    }

    /** Un joueur entre dans la salle d'attente de l'arène. */
    public CompletableFuture<Void> join(String gameId, String playerId) {
        return commandGateway
                .send(new GameCommand.JoinGameCommand(gameId, playerId))
                .thenAccept(_ -> {
                });
    }

    /** Un joueur quitte la salle d'attente avant le démarrage (annule la partie). */
    public CompletableFuture<Void> leave(String gameId, String playerId) {
        return commandGateway
                .send(new GameCommand.LeaveGameCommand(gameId, playerId))
                .thenAccept(_ -> {
                });
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
     * progression courante (niveau, titre, palier). La partie doit exister et le joueur en
     * faire partie, sinon le service game répond un {@code Problem} métier.
     */
    public CompletableFuture<GameResultView> result(String gameId, String playerId) {
        CompletableFuture<GameResult> resultFuture = queryGateway.query(
                new GameQuery.GetGameResultQuery(gameId, playerId),
                QueryResponseTypes.instanceOf(GameResult.class));
        CompletableFuture<List<GameXp>> xpFuture = queryGateway.query(
                new ProgressionQuery.GetGamesXpQuery(playerId, List.of(gameId)),
                QueryResponseTypes.multipleInstancesOf(GameXp.class));
        CompletableFuture<PlayerProgress> progressFuture = queryGateway.query(
                new ProgressionQuery.GetProgressionQuery(playerId),
                QueryResponseTypes.instanceOf(PlayerProgress.class));

        return CompletableFuture.allOf(resultFuture, xpFuture, progressFuture)
                .thenApply(_ -> {
                    GameResult gameResult = resultFuture.join();
                    GameXp gameXp = xpFuture.join().stream().findFirst().orElse(null);
                    Integer xp = gameXp == null ? null : gameXp.xp();
                    GameResultView.RewardView reward = xp == null
                            ? null
                            : new GameResultView.RewardView(xp, xp - gameResult.myScore());
                    ProgressionView progression = ProgressionViews.toView(progressFuture.join());
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
                            new GameResultView.ProgressionResultView(
                                    progression.xpTotal(),
                                    progression.level(),
                                    progression.title(),
                                    progression.xpForNextLevel(),
                                    progression.levelProgressPercent()));
                });
    }

    /**
     * Demande de revanche : les langues portées par la commande sont l'union des langues
     * actuelles des deux joueurs (résolues via leur profil). Pour une partie bot, seul le
     * demandeur a un profil ; l'agrégat refusera de toute façon la revanche contre un bot.
     */
    public CompletableFuture<Void> requestRematch(String gameId, String playerId) {
        return queryGateway
                .query(new GameQuery.GetGameByIdQuery(gameId), QueryResponseTypes.instanceOf(Game.class))
                .thenCompose(game -> {
                    String opponentId = opponentIdOf(playerId, game);
                    if (opponentId == null) {
                        return profileLookup.get(playerId)
                                .thenCompose(player -> sendRematchRequest(gameId, playerId, Set.of(player.language())));
                    }
                    CompletableFuture<Profile> playerFuture = profileLookup.get(playerId);
                    CompletableFuture<Profile> opponentFuture = profileLookup.get(opponentId);
                    return CompletableFuture.allOf(playerFuture, opponentFuture)
                            .thenCompose(_ -> {
                                Set<Language> languages = EnumSet.of(
                                        playerFuture.join().language(),
                                        opponentFuture.join().language());
                                return sendRematchRequest(gameId, playerId, languages);
                            });
                });
    }

    /** L'invité accepte la revanche demandée. */
    public CompletableFuture<Void> acceptRematch(String gameId, String playerId) {
        return commandGateway
                .send(new GameCommand.AcceptRematchCommand(gameId, playerId))
                .thenAccept(_ -> {
                });
    }

    /** L'invité refuse la revanche demandée. */
    public CompletableFuture<Void> declineRematch(String gameId, String playerId) {
        return commandGateway
                .send(new GameCommand.DeclineRematchCommand(gameId, playerId))
                .thenAccept(_ -> {
                });
    }

    /** Le demandeur retire sa demande de revanche. */
    public CompletableFuture<Void> cancelRematch(String gameId, String playerId) {
        return commandGateway
                .send(new GameCommand.CancelRematchCommand(gameId, playerId))
                .thenAccept(_ -> {
                });
    }

    private CompletableFuture<Void> sendRematchRequest(String gameId, String playerId, Set<Language> languages) {
        return commandGateway
                .send(new GameCommand.RequestRematchCommand(gameId, playerId, languages))
                .thenAccept(_ -> {
                });
    }
}
