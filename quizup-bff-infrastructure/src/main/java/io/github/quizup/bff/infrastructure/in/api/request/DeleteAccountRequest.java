package io.github.quizup.bff.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Confirmation d'une suppression de compte : code OTP + phrase de confirmation. */
public record DeleteAccountRequest(
        @NotBlank @Size(max = 20) String confirmation,
        @NotBlank @Size(min = 6, max = 6) String code
) {
}
