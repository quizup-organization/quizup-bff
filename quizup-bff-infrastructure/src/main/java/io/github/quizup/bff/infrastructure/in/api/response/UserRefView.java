package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Référence minimale d'un joueur (adversaire, participant) : identité + avatar.
 */
public record UserRefView(
        String userId,
        String pseudonym,
        String avatarOptions
) {
}
