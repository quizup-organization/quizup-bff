package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.PresenceStatus;

import java.time.Instant;

/**
 * Présence d'un joueur (REST {@code /api/presence/{id}} et push WS {@code /topic/presence/{id}}).
 */
public record PresenceView(
        String userId,
        PresenceStatus status,
        Instant lastSeenAt
) {
}
