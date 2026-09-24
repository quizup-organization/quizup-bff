package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.SocialNotification;
import io.github.quizup.social.domain.event.ChallengeEvent;

import java.util.Optional;

import static java.util.Objects.isNull;

public final class SocialEventNotificationMapper {

    private SocialEventNotificationMapper() {
    }

    public static Optional<SocialNotification> toNotification(ChallengeEvent event) {
        if (isNull(event)) {
            return Optional.empty();
        }

        return switch (event) {
            case ChallengeEvent.ChallengeCreatedEvent e -> Optional.of(
                    new SocialNotification.ChallengeReceivedNotification(
                            e.challengeId(),
                            e.challengerId(),
                            e.topicId(),
                            e.challengedId(),
                            e.expiresAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeAcceptedEvent e -> Optional.of(
                    new SocialNotification.ChallengeAcceptedNotification(
                            e.challengeId(),
                            e.gameId(),
                            e.challengedId(),
                            e.challengerId(),
                            e.acceptedAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeDeclinedEvent e -> Optional.of(
                    new SocialNotification.ChallengeDeclinedNotification(
                            e.challengeId(),
                            e.challengedId(),
                            e.challengerId(),
                            e.declinedAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeExpiredEvent e -> Optional.of(
                    new SocialNotification.ChallengeExpiredNotification(
                            e.challengeId(),
                            e.challengerId(),
                            e.expiredAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeCanceledEvent e -> Optional.of(
                    new SocialNotification.ChallengeCanceledNotification(
                            e.challengeId(),
                            e.challengerId(),
                            e.challengedId(),
                            e.canceledAt().toString()
                    )
            );
            default -> Optional.empty();
        };
    }
}
