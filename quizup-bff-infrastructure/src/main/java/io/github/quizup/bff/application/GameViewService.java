package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.bff.infrastructure.in.api.response.CurrentGameView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
     * Partie en attente/en cours du joueur (bannière de reprise). Absence ⇒
     * {@code NoCurrentGameProblem} (404) côté query bus.
     */
    public CompletableFuture<CurrentGameView> current(String userId) {
        return queryGateway
                .query(new GameQuery.GetCurrentGameQuery(userId), QueryResponseTypes.instanceOf(Game.class))
                .thenCompose(game -> {
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
}
