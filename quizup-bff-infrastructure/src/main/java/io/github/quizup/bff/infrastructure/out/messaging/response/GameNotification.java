package io.github.quizup.bff.infrastructure.out.messaging.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.quizup.game.domain.model.GameQuestionChoice;

import java.time.Instant;
import java.util.Map;

/**
 * Notifications de partie (rapatriées du service game ; le BFF est la seule surface WS).
 * Le discriminant est exposé explicitement par {@code type} ({@link JsonProperty}).
 */
public interface GameNotification {

    @JsonProperty("type")
    GameNotificationType type();

    String gameId();

    enum GameNotificationType {
        GAME_CREATED,
        PLAYER_JOINED,
        GAME_STARTED,
        ROUND_STARTED,
        QUESTION_REVEALED,
        PLAYER_ANSWERED,
        ROUND_CLOSED,
        GAME_ENDED,
        GAME_RUN_RECORDED,
        GAME_CANCELLED
    }

    record GameCreatedNotification(
            String gameId,
            String topicId,
            String player1Id,
            String player1Name,
            String player2Id,
            String player2Name,
            String player2Type,
            String mode,
            String botDifficulty
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_CREATED;
        }
    }

    record PlayerJoinedNotification(
            String gameId,
            String playerId
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.PLAYER_JOINED;
        }
    }

    record GameStartedNotification(
            String gameId,
            String mode,
            Instant firstRoundAt
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_STARTED;
        }
    }

    record RoundStartedNotification(
            String gameId,
            String round,
            String questionId,
            String questionText,
            String imageUrl,
            String difficulty,
            Map<String, String> answers,
            boolean bonus,
            Instant shownAt,
            Instant revealAt
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.ROUND_STARTED;
        }
    }

    record QuestionRevealedNotification(
            String gameId,
            String round,
            Instant revealedAt,
            Instant answerDeadlineAt
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.QUESTION_REVEALED;
        }
    }

    record PlayerAnsweredNotification(
            String gameId,
            String round,
            String playerId,
            GameQuestionChoice choice,
            boolean correct,
            int pointsEarned,
            Instant answeredAt,
            long timeMs
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.PLAYER_ANSWERED;
        }
    }

    record RoundClosedNotification(
            String gameId,
            String closedRound,
            String nextRound,
            GameQuestionChoice correctAnswer,
            Instant closedAt,
            Instant nextRoundAt
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.ROUND_CLOSED;
        }
    }

    record GameEndedNotification(
            String gameId,
            String winnerId,
            int player1FinalScore,
            int player2FinalScore
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_ENDED;
        }
    }

    record GameRunRecordedNotification(
            String gameId,
            String playerId,
            int score
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_RUN_RECORDED;
        }
    }

    record GameCancelledNotification(
            String gameId,
            String reason
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_CANCELLED;
        }
    }
}
