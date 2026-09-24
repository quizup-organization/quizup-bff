package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.GameNotification;
import io.github.quizup.game.domain.event.GameEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;

public final class GameEventNotificationMapper {

    private GameEventNotificationMapper() {
    }

    public static Optional<GameNotification> toNotification(GameEvent event) {
        if (isNull(event)) {
            return Optional.empty();
        }

        return switch (event) {
            case GameEvent.GameCreatedEvent gameCreatedEvent -> Optional.of(
                    new GameNotification.GameCreatedNotification(
                            gameCreatedEvent.gameId(),
                            gameCreatedEvent.topicId(),
                            gameCreatedEvent.player1Id(),
                            gameCreatedEvent.player1Name(),
                            gameCreatedEvent.player2Id(),
                            gameCreatedEvent.player2Name(),
                            gameCreatedEvent.player2Type() != null ? gameCreatedEvent.player2Type().name() : null,
                            gameCreatedEvent.mode() != null ? gameCreatedEvent.mode().name() : null,
                            gameCreatedEvent.botDifficulty() != null ? gameCreatedEvent.botDifficulty().name() : null
                    )
            );

            case GameEvent.GameJoinedEvent gameJoinedEvent -> Optional.of(
                    new GameNotification.PlayerJoinedNotification(
                            gameJoinedEvent.gameId(),
                            gameJoinedEvent.playerId()
                    )
            );

            case GameEvent.GameStartedEvent gameStartedEvent -> Optional.of(
                    new GameNotification.GameStartedNotification(
                            gameStartedEvent.gameId(),
                            gameStartedEvent.mode().name(),
                            gameStartedEvent.firstRoundAt()
                    )
            );

            case GameEvent.RoundStartedEvent roundStartedEvent -> {
                Map<String, String> answers = new LinkedHashMap<>();
                roundStartedEvent
                        .question()
                        .answers()
                        .forEach((choice, text) -> answers.put(choice.name(), text));
                yield Optional.of(
                        new GameNotification.RoundStartedNotification(
                                roundStartedEvent.gameId(),
                                roundStartedEvent.round().name(),
                                roundStartedEvent.question().questionId(),
                                roundStartedEvent.question().text(),
                                roundStartedEvent.question().imageUrl(),
                                roundStartedEvent.question().difficulty(),
                                answers,
                                roundStartedEvent.round().isBonus(),
                                roundStartedEvent.shownAt(),
                                roundStartedEvent.revealAt()
                        ));
            }

            case GameEvent.QuestionRevealedEvent questionRevealedEvent -> Optional.of(
                    new GameNotification.QuestionRevealedNotification(
                            questionRevealedEvent.gameId(),
                            questionRevealedEvent.round().name(),
                            questionRevealedEvent.revealedAt(),
                            questionRevealedEvent.answerDeadlineAt()
                    )
            );

            case GameEvent.QuestionAnsweredEvent questionAnsweredEvent -> Optional.of(
                    new GameNotification.PlayerAnsweredNotification(
                            questionAnsweredEvent.gameId(),
                            questionAnsweredEvent.round().name(),
                            questionAnsweredEvent.playerId(),
                            questionAnsweredEvent.choice(),
                            questionAnsweredEvent.correct(),
                            questionAnsweredEvent.pointsEarned(),
                            questionAnsweredEvent.answeredAt(),
                            questionAnsweredEvent.timeMs()
                    )
            );

            case GameEvent.RoundClosedEvent roundClosedEvent -> Optional.of(
                    new GameNotification.RoundClosedNotification(
                            roundClosedEvent.gameId(),
                            roundClosedEvent.closedRound().name(),
                            roundClosedEvent.nextRound() != null ? roundClosedEvent.nextRound().name() : null,
                            roundClosedEvent.correctAnswer(),
                            roundClosedEvent.closedAt(),
                            roundClosedEvent.nextRoundAt()
                    )
            );

            case GameEvent.GameEndedEvent gameEndedEvent -> Optional.of(
                    new GameNotification.GameEndedNotification(
                            gameEndedEvent.gameId(),
                            gameEndedEvent.winnerId(),
                            gameEndedEvent.player1FinalScore(),
                            gameEndedEvent.player2FinalScore()
                    )
            );

            case GameEvent.GameCancelledEvent gameCancelledEvent -> Optional.of(
                    new GameNotification.GameCancelledNotification(
                            gameCancelledEvent.gameId(),
                            gameCancelledEvent.reason()
                    )
            );

            case GameEvent.GameRunRecordedEvent gameRunRecordedEvent -> Optional.of(
                    new GameNotification.GameRunRecordedNotification(
                            gameRunRecordedEvent.gameId(),
                            gameRunRecordedEvent.playerId(),
                            gameRunRecordedEvent.score()
                    )
            );

            default -> Optional.empty();
        };
    }
}
