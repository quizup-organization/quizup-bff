package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.notification.domain.model.NotificationCategory;

/** Préférence d'une catégorie de notification pour le joueur courant. */
public record NotificationPreferenceView(
        NotificationCategory category,
        boolean enabled
) {
}
