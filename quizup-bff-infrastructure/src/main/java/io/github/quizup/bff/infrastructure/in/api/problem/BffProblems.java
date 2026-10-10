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

    public static class InvalidGameListRequestProblem extends BaseProblem {
        public InvalidGameListRequestProblem(String detail) {
            super("urn:quizup:bff:game:invalidListRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid game list request",
                    detail,
                    Map.of());
        }
    }

    public static class InvalidTopicListRequestProblem extends BaseProblem {
        public InvalidTopicListRequestProblem(String detail) {
            super("urn:quizup:bff:topic:invalidListRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid topic list request",
                    detail,
                    Map.of());
        }
    }

    public static class InvalidQuestionRequestProblem extends BaseProblem {
        public InvalidQuestionRequestProblem(String detail) {
            super("urn:quizup:bff:question:invalidRequest",
                    ProblemCategory.VALIDATION,
                    "Invalid question request",
                    detail,
                    Map.of());
        }
    }

    public static class PushDisabledProblem extends BaseProblem {
        public PushDisabledProblem() {
            super("urn:quizup:bff:push:disabled",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Web Push disabled",
                    "Web Push is not configured on this server",
                    Map.of());
        }
    }

    public static class NotTopicOwnerProblem extends BaseProblem {
        public NotTopicOwnerProblem(String topicId) {
            super("urn:quizup:bff:topic:notOwner",
                    ProblemCategory.PERMISSION,
                    "Topic management not allowed",
                    "Only the creator of the topic can manage it",
                    Map.of("topicId", topicId));
        }
    }
}
