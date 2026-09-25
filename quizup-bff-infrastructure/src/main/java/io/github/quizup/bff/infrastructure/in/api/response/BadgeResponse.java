package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * DTO d'un badge débloqué (code technique + libellé d'affichage).
 */
public record BadgeResponse(
        String code,
        String label
) implements Serializable {
}
