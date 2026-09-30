package io.github.quizup.bff.infrastructure.in.api.request;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code PUT /api/profiles/{userId}/language}.
 */
public record UpdateLanguageRequest(
        @NotNull Language language
) {
}
