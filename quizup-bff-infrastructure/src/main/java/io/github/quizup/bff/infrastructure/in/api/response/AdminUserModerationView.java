package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.UserSuspensionReason;

import java.time.Instant;

/** Résultat d'une suspension ou d'une réintégration. */
public record AdminUserModerationView(
        String userId,
        AccountStatus status,
        Instant suspendedUntil,
        UserSuspensionReason suspensionReason,
        int resolvedReports,
        boolean protectedAccount
) {
}
