package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Écritures de duel : création (bot / run asynchrone / replay fantôme), réponse, abandon.
 */
@Service
public class GameViewService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public GameViewService(QueryGateway queryGateway, CommandGateway commandGateway, ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    public CompletableFuture<String> create(String playerId, CreateGameRequest request) {
        return profileLookup.get(playerId).thenCompose(player -> switch (request.mode()) {
            case BOT -> createBotGame(player, request);
            case ASYNC -> createAsyncGame(player, request);
        });
    }

    public CompletableFuture<Void> answer(String gameId, String playerId, GameQuestionChoice choice) {
        return commandGateway
                .send(new GameCommand.AnswerQuestionCommand(gameId, playerId, choice, Instant.now()))
                .thenAccept(_ -> {
                });
    }

    /**
     * Abandon toujours valide : partie non démarrée → annulation ; partie en cours → forfait.
     */
    public CompletableFuture<Void> abandon(String gameId, String playerId) {
        return queryGateway
                .query(new GameQuery.GetGameByIdQuery(gameId), QueryResponseTypes.instanceOf(Game.class))
                .thenCompose(game -> {
                    if (game.status() == GameStatus.CREATED || game.status() == GameStatus.READY) {
                        return commandGateway.send(new GameCommand.CancelGameCommand(gameId, "PLAYER_CANCELLED"));
                    }
                    if (game.status() == GameStatus.IN_PROGRESS || game.status() == GameStatus.AWAITING_OPPONENT) {
                        return commandGateway.send(new GameCommand.EndGameCommand(gameId, playerId));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenAccept(_ -> {
                });
    }

    public CompletableFuture<Void> cancel(String gameId) {
        return commandGateway
                .send(new GameCommand.CancelGameCommand(gameId, "PLAYER_CANCELLED"))
                .thenAccept(_ -> {
                });
    }

    private CompletableFuture<String> createBotGame(Profile player, CreateGameRequest request) {
        if (request.opponentId() != null || request.ghostGameId() != null) {
            throw new BffProblems.InvalidGameRequestProblem(
                    "Un duel bot ne prend ni adversaire ni partie fantôme");
        }
        String gameId = UUID.randomUUID().toString();
        GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                gameId,
                request.topicId(),
                player.userId(),
                player.pseudonym(),
                QuizUpConstants.SYSTEM_USER_ID,
                QuizUpConstants.SYSTEM_USER_NAME,
                GameMode.SYNC,
                Set.of(player.language()),
                GamePlayerType.BOT,
                BotDifficulty.fromOrDefault(request.difficulty()),
                null);
        return commandGateway.send(command).thenApply(_ -> gameId);
    }

    private CompletableFuture<String> createAsyncGame(Profile player, CreateGameRequest request) {
        boolean hasGhost = request.ghostGameId() != null && !request.ghostGameId().isBlank();
        boolean hasOpponent = request.opponentId() != null && !request.opponentId().isBlank();
        if (hasGhost != hasOpponent) {
            throw new BffProblems.InvalidGameRequestProblem(
                    "Un replay fantôme exige l'adversaire d'origine (opponentId + ghostGameId)");
        }

        String gameId = UUID.randomUUID().toString();
        if (!hasGhost) {
            GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                    gameId,
                    request.topicId(),
                    player.userId(),
                    player.pseudonym(),
                    null,
                    null,
                    GameMode.ASYNC,
                    Set.of(player.language()),
                    GamePlayerType.HUMAN,
                    null,
                    null);
            return commandGateway.send(command).thenApply(_ -> gameId);
        }

        return profileLookup.get(request.opponentId()).thenCompose(opponent -> {
            GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                    gameId,
                    request.topicId(),
                    player.userId(),
                    player.pseudonym(),
                    opponent.userId(),
                    opponent.pseudonym(),
                    GameMode.ASYNC,
                    Set.of(player.language()),
                    GamePlayerType.GHOST,
                    null,
                    request.ghostGameId());
            return commandGateway.send(command).thenApply(_ -> gameId);
        });
    }
}
