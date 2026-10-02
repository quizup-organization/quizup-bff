package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.CreateGameRequest;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.model.Profile;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Écritures de duel : création d'un duel bot, entrée/sortie de la salle d'attente,
 * réponse, abandon, annulation.
 */
@Service
public class GameViewService {

    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public GameViewService(CommandGateway commandGateway, ProfileLookup profileLookup) {
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
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
