package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.infrastructure.in.api.response.AchievementView;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.MasteryGrade;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Fan-out éphémère des déblocages : chaque événement du profile part sur le topic du joueur
 * concerné, avec le détail nécessaire à la célébration côté client.
 */
class AchievementPublisherTest {

    private static final String USER_ID = "user-1";
    private static final Instant AT = Instant.parse("2026-10-15T18:00:00Z");
    private static final String DESTINATION = "/topic/achievements/" + USER_ID;

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final AchievementPublisher publisher = new AchievementPublisher(messagingTemplate);

    @Test
    void badgeEarned_publishesBadgeAchievement() {
        publisher.onBadgeEarned(new ProgressionEvent.BadgeEarnedEvent(
                USER_ID, Badge.DUELIST_DIAMANT, AT));

        verify(messagingTemplate).convertAndSend(eq(DESTINATION), eq(AchievementView.badge(
                "DUELIST_DIAMANT", "Mythe vivant", "VOLUME", "DIAMANT",
                Badge.DUELIST_DIAMANT.metric().name(), Badge.DUELIST_DIAMANT.target(), AT)));
    }

    @Test
    void masteryGradeReached_publishesTitleAchievement() {
        publisher.onMasteryGradeReached(new ProgressionEvent.MasteryGradeReachedEvent(
                USER_ID, "topic-1", MasteryGrade.CONFIRME, AT));

        verify(messagingTemplate).convertAndSend(eq(DESTINATION),
                eq(AchievementView.title("topic-1", "CONFIRME", AT)));
    }

    @Test
    void publishesNothingWithoutUserId() {
        publisher.onBadgeEarned(new ProgressionEvent.BadgeEarnedEvent(
                null, Badge.FIRST_WIN, AT));

        verifyNoInteractions(messagingTemplate);
    }
}
