package io.github.quizup.bff.infrastructure.out.messaging.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Notifications sociales (défis) — contrat web du BFF ; diffusées sur {@code /topic/social/{userId}}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SocialNotification.ChallengeReceivedNotification.class, name = "CHALLENGE_RECEIVED"),
        @JsonSubTypes.Type(value = SocialNotification.ChallengeAcceptedNotification.class, name = "CHALLENGE_ACCEPTED"),
        @JsonSubTypes.Type(value = SocialNotification.ChallengeDeclinedNotification.class, name = "CHALLENGE_DECLINED"),
        @JsonSubTypes.Type(value = SocialNotification.ChallengeCanceledNotification.class, name = "CHALLENGE_CANCELED"),
        @JsonSubTypes.Type(value = SocialNotification.ChallengeExpiredNotification.class, name = "CHALLENGE_EXPIRED"),
        @JsonSubTypes.Type(value = SocialNotification.ChallengeCompletedNotification.class, name = "CHALLENGE_COMPLETED")
})
public interface SocialNotification {

    @JsonProperty("type")
    SocialNotificationType type();

    String userId();

    enum SocialNotificationType {
        CHALLENGE_RECEIVED,
        CHALLENGE_ACCEPTED,
        CHALLENGE_DECLINED,
        CHALLENGE_CANCELED,
        CHALLENGE_EXPIRED,
        CHALLENGE_COMPLETED
    }

    record ChallengeReceivedNotification(
            String challengeId,
            String challengerId,
            String topicId,
            String userId,
            String expiresAt
    ) implements SocialNotification {
        @Override
        public SocialNotificationType type() {
            return SocialNotificationType.CHALLENGE_RECEIVED;
        }
    }

    record ChallengeAcceptedNotification(
            String challengeId,
            String gameId,
            String acceptedBy,
            String userId,
            String timestamp
    ) implements SocialNotification {
        @Override
        public SocialNotificationType type() {
            return SocialNotificationType.CHALLENGE_ACCEPTED;
        }
    }

    record ChallengeDeclinedNotification(
            String challengeId,
            String declinedBy,
            String userId,
            String timestamp
    ) implements SocialNotification {
        @Override
        public SocialNotificationType type() {
            return SocialNotificationType.CHALLENGE_DECLINED;
        }
    }

    record ChallengeCanceledNotification(
            String challengeId,
            String canceledBy,
            String userId,
            String timestamp
    ) implements SocialNotification {
        @Override
        public SocialNotificationType type() {
            return SocialNotificationType.CHALLENGE_CANCELED;
        }
    }

    record ChallengeExpiredNotification(
            String challengeId,
            String userId,
            String timestamp
    ) implements SocialNotification {
        @Override
        public SocialNotificationType type() {
            return SocialNotificationType.CHALLENGE_EXPIRED;
        }
    }

    record ChallengeCompletedNotification(
            String challengeId,
            String winnerId,
            int challengerScore,
            int challengedScore,
            String userId,
            String timestamp
    ) implements SocialNotification {
        @Override
        public SocialNotificationType type() {
            return SocialNotificationType.CHALLENGE_COMPLETED;
        }
    }
}
