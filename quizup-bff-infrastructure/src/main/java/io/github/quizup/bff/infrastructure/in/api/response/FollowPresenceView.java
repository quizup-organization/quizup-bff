package io.github.quizup.bff.infrastructure.in.api.response;

import java.time.Instant;

/**
 * Passage en ligne d'un joueur suivi, poussé de façon **éphémère** sur
 * {@code /topic/follow-presence/{userId}} (userId = l'abonné à notifier). Jamais persisté :
 * aucun passage par l'inbox de {@code quizup-notification}.
 */
public record FollowPresenceView(
        String actorId,
        String pseudonym,
        String avatarOptions,
        Instant at
) {
}
