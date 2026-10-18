package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.model.UserSuspensionReason;

import java.time.Instant;

/**
 * Compte vu par l'administration : statut effectif et métadonnées de la dernière suspension.
 */
public record AdminUserAccountView(
        String userId,
        String email,
        Instant createdAt,
        AccountStatus status,
        boolean suspended,
        Instant suspendedUntil,
        UserSuspensionReason suspensionReason,
        String suspensionNote,
        Instant suspendedAt,
        String suspendedBy,
        Instant reinstatedAt,
        String reinstatedBy,
        SuspensionReinstatementReason reinstatementReason,
        String reinstatementNote,
        boolean protectedAccount
) {
}
