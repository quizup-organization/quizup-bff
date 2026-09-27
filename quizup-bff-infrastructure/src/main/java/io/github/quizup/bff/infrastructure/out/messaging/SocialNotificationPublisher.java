package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.SocialEventNotificationMapper;
import io.github.quizup.bff.infrastructure.out.messaging.response.SocialNotification;
import io.github.quizup.social.domain.event.ChallengeEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Diffuse les notifications sociales (défis) sur {@code /topic/social/{userId}}.
 */
@Service
@ProcessingGroup("social-notification")
public class SocialNotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SocialNotificationPublisher.class);
    private static final String DESTINATION_PREFIX = "/topic/social/";

    private final SimpMessagingTemplate messagingTemplate;

    public SocialNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void onChallengeEvent(EventMessage<?> eventMessage) {
        if (!(eventMessage.getPayload() instanceof ChallengeEvent event)) {
            return;
        }
        List<SocialNotification> notifications = SocialEventNotificationMapper.toNotifications(event);
        if (notifications.isEmpty()) {
            logger.debug("Aucune notification pour l'événement de défi: {}", event.getClass().getSimpleName());
            return;
        }
        notifications.forEach(notification -> send(event, eventMessage, notification));
    }

    private void send(ChallengeEvent event, EventMessage<?> eventMessage, SocialNotification notification) {
        if (!(eventMessage instanceof DomainEventMessage<?> domainMessage)) {
            logger.warn("Notification ignorée (événement sans métadonnées d'agrégat): {}", event.getClass().getSimpleName());
            return;
        }

        EventEnvelopeResponse envelope = EventEnvelopeResponse.of(
                event.challengeId(),
                domainMessage.getSequenceNumber(),
                domainMessage.getTimestamp(),
                notification.type().name(),
                notification
        );

        messagingTemplate.convertAndSend(DESTINATION_PREFIX + notification.userId(), envelope);
    }
}
