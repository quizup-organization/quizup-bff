package io.github.quizup.bff.infrastructure.out.messaging.mapper;

import io.github.quizup.bff.infrastructure.out.messaging.response.SocialNotification;
import io.github.quizup.social.domain.event.ChallengeEvent;

import java.util.List;

import static java.util.Objects.isNull;

public final class SocialEventNotificationMapper {

    private SocialEventNotificationMapper() {
    }

    /**
     * Traduit un événement de défi en notifications destinées à chaque joueur concerné
     * (une seule en général, deux pour la complétion). Les événements internes
     * (enregistrement de run/résultat) ne produisent aucune notification.
     */
    public static List<SocialNotification> toNotifications(ChallengeEvent event) {
        if (isNull(event)) {
            return List.of();
        }

        return switch (event) {
            case ChallengeEvent.ChallengeCreatedEvent e -> List.of(
                    new SocialNotification.ChallengeReceivedNotification(
                            e.challengeId(),
                            e.challengerId(),
                            e.topicId(),
                            e.challengedId(),
                            e.expiresAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeAcceptedEvent e -> List.of(
                    new SocialNotification.ChallengeAcceptedNotification(
                            e.challengeId(),
                            e.gameId(),
                            e.challengedId(),
                            e.challengerId(),
                            e.acceptedAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeDeclinedEvent e -> List.of(
                    new SocialNotification.ChallengeDeclinedNotification(
                            e.challengeId(),
                            e.challengedId(),
                            e.challengerId(),
                            e.declinedAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeExpiredEvent e -> List.of(
                    new SocialNotification.ChallengeExpiredNotification(
                            e.challengeId(),
                            e.challengerId(),
                            e.expiredAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeCanceledEvent e -> List.of(
                    new SocialNotification.ChallengeCanceledNotification(
                            e.challengeId(),
                            e.challengerId(),
                            e.challengedId(),
                            e.canceledAt().toString()
                    )
            );
            case ChallengeEvent.ChallengeCompletedEvent e -> List.of(
                    completedNotification(e, e.challengerId()),
                    completedNotification(e, e.challengedId())
            );
            default -> List.of();
        };
    }

    private static SocialNotification completedNotification(ChallengeEvent.ChallengeCompletedEvent event,
                                                            String userId) {
        return new SocialNotification.ChallengeCompletedNotification(
                event.challengeId(),
                event.winnerId(),
                event.challengerScore(),
                event.challengedScore(),
                userId,
                event.completedAt().toString()
        );
    }
}
