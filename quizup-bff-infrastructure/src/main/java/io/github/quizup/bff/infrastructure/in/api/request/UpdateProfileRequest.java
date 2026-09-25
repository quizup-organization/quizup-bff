package io.github.quizup.bff.infrastructure.in.api.request;

/**
 * Corps de {@code PUT /api/profiles/{userId}} — mise à jour du profil par son propriétaire.
 */
public record UpdateProfileRequest(
        String displayName,
        String bio,
        String country,
        String avatarOptions
) {
}
