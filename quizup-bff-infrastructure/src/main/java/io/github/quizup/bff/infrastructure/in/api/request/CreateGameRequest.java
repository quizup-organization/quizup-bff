package io.github.quizup.bff.infrastructure.in.api.request;

/**
 * Création unifiée d'une partie : {@code mode} = {@code BOT} (défaut) ou {@code ASYNC}.
 * Pour un replay asynchrone, fournir {@code ghostGameId} (+ {@code opponentId}/{@code opponentName}).
 */
public record CreateGameRequest(
        String topicId,
        String mode,
        String difficulty,
        String opponentId,
        String opponentName,
        String ghostGameId
) {
}
