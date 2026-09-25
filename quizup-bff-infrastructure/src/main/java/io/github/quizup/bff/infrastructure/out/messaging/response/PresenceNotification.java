package io.github.quizup.bff.infrastructure.out.messaging.response;

import io.github.quizup.profile.domain.model.PresenceStatus;

import java.time.Instant;

/**
 * Notification temps réel de présence, diffusée sur {@code /topic/presence/{userId}}.
 */
public record PresenceNotification(
        String userId,
        PresenceStatus status,
        Instant lastSeenAt
) {
}
