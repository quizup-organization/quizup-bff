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
                null,
                null,
                Instant.parse("2026-09-18T10:00:00Z")
        );

        GameNotification notification = GameEventNotificationMapper.toNotification(event).orElseThrow();

        assertThat(notification).isInstanceOf(GameNotification.GameCreatedNotification.class);
        GameNotification.GameCreatedNotification created = (GameNotification.GameCreatedNotification) notification;
        assertThat(created.questionImageUrls())
                .containsExactly("https://images.example/q1.png", "https://images.example/q3.png");
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
