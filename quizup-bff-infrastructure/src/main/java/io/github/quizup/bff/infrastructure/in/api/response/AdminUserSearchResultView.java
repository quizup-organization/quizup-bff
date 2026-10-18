package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.identity.domain.model.AccountStatus;

import java.time.Instant;

/** Résultat de la recherche admin d'un joueur (pseudo, email ou identifiant). */
public record AdminUserSearchResultView(
        String userId,
        String email,
        String pseudonym,
        String avatarOptions,
        AccountStatus status,
        boolean suspended,
        Instant suspendedUntil,
        boolean protectedAccount
) {
}
