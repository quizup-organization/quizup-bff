package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.GameNotification;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameQuestionContent;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le client web précharge les images dès la création de la partie : le mapping
 * {@code GAME_CREATED} doit exposer toutes les images non nulles, dans l'ordre des rounds.
 */
class GameEventNotificationMapperTest {

    @Test
    void game_created_exposes_non_null_question_images_in_round_order() {
        GameEvent.GameCreatedEvent event = new GameEvent.GameCreatedEvent(
                "game-1",
                "topic-1",
                "player-1",
                "Alice",
                "player-2",
                "Bob",
                GamePlayerType.HUMAN,
                List.of(
                        question("q1", "https://images.example/q1.png"),
                        question("q2", null),
                        question("q3", "https://images.example/q3.png")
                ),
                null,
                Instant.parse("2026-09-18T10:00:00Z")
        );

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isInstanceOf(GameNotification.GameCreatedNotification.class);
        GameNotification.GameCreatedNotification created = (GameNotification.GameCreatedNotification) notification;
        assertThat(created.questionImageUrls())
                .containsExactly("https://images.example/q1.png", "https://images.example/q3.png");
    }

    @Test
    void rematch_requested_maps_requester_and_type() {
        GameEvent.RematchRequestedEvent event = new GameEvent.RematchRequestedEvent(
                "game-1", "player-1", "Alice", "player-2", "Bob", "topic-1",
                Set.of(Language.FR), Instant.parse("2026-10-05T12:00:00Z"));

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isEqualTo(new GameNotification.RematchRequestedNotification("game-1", "player-1"));
        assertThat(notification.type()).isEqualTo(GameNotification.GameNotificationType.REMATCH_REQUESTED);
    }

    @Test
    void rematch_accepted_maps_player_and_type() {
        GameEvent.RematchAcceptedEvent event = new GameEvent.RematchAcceptedEvent(
                "game-1", "player-2", Instant.parse("2026-10-05T12:00:05Z"));

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isEqualTo(new GameNotification.RematchAcceptedNotification("game-1", "player-2"));
        assertThat(notification.type()).isEqualTo(GameNotification.GameNotificationType.REMATCH_ACCEPTED);
    }

    @Test
    void rematch_declined_maps_player_and_type() {
        GameEvent.RematchDeclinedEvent event = new GameEvent.RematchDeclinedEvent(
                "game-1", "player-2", Instant.parse("2026-10-05T12:00:05Z"));

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isEqualTo(new GameNotification.RematchDeclinedNotification("game-1", "player-2"));
        assertThat(notification.type()).isEqualTo(GameNotification.GameNotificationType.REMATCH_DECLINED);
    }

    @Test
    void rematch_cancelled_maps_reason_and_type() {
        GameEvent.RematchCancelledEvent event = new GameEvent.RematchCancelledEvent(
                "game-1", "REQUESTER_LEFT", Instant.parse("2026-10-05T12:00:05Z"));

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isEqualTo(new GameNotification.RematchCancelledNotification("game-1", "REQUESTER_LEFT"));
        assertThat(notification.type()).isEqualTo(GameNotification.GameNotificationType.REMATCH_CANCELLED);
    }

    @Test
    void rematch_started_maps_new_game_and_type() {
        GameEvent.RematchStartedEvent event = new GameEvent.RematchStartedEvent(
                "game-1", "game-2", Instant.parse("2026-10-05T12:00:05Z"));

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isEqualTo(new GameNotification.RematchStartedNotification("game-1", "game-2"));
        assertThat(notification.type()).isEqualTo(GameNotification.GameNotificationType.REMATCH_STARTED);
    }

    private static GameQuestion question(String questionId, String imageUrl) {
        return new GameQuestion(
                questionId,
                Map.of(Language.FR, new GameQuestionContent(
                        "Question " + questionId,
                        Map.of(GameQuestionChoice.A, "Réponse A")
                )),
                imageUrl,
                null,
                GameQuestionChoice.A
        );
    }
}
