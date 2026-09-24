package io.github.quizup.bff.infrastructure.in.api.request;

public record CreateChallengeRequest(
        String challengedId,
        String topicId
) {
}
