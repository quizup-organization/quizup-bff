package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.identity.domain.model.UserReportRules;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;

/**
 * Suspension d'un compte.
 *
 * @param reason            motif public
 * @param note              note interne admin (jamais exposée au joueur)
 * @param duration          durée ISO-8601 (ex. {@code P7D}) ; interdite si {@code permanent}
 * @param permanent         suspension définitive (aucune échéance)
 * @param resolveOpenReports clôture les signalements ouverts du joueur après la suspension
 */
public record SuspendUserRequest(
        @NotNull UserSuspensionReason reason,
        @Size(max = UserReportRules.MAX_NOTE_LENGTH) String note,
        Duration duration,
        boolean permanent,
        boolean resolveOpenReports
) {
}
