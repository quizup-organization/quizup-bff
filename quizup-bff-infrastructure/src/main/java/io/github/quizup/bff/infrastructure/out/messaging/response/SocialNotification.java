package io.github.quizup.bff.infrastructure.out.messaging.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Notifications sociales (défis), rapatriées du service social ; diffusées sur
 * {@code /topic/social/{userId}}.
 */
public interface SocialNotification {

    @JsonProperty("type")
    SocialNotificationType type();

    String userId();

    enum SocialNotificationType {
        CHALLENGE_RECEIVED,
        CHALLENGE_ACCEPTED,
        CHALLENGE_DECLINED,
        CHALLENGE_CANCELED,
        CHALLENGE_EXPIRED
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
}
