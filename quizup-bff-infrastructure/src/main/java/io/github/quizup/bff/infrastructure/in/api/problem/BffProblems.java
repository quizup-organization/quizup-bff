package io.github.quizup.bff.infrastructure.in.api.problem;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;

/**
 * Problèmes de façade propres au BFF (validation de requête, ressource absente).
 */
public final class BffProblems {

    private BffProblems() {
    }

    public static class InvalidGameRequestProblem extends BaseProblem {
        public InvalidGameRequestProblem(String detail) {
            super("urn:quizup:bff:game:invalidRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid game request",
                    detail,
                    Map.of());
        }
    }
}
