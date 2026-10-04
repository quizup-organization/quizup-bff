package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.in.api.response.NotificationDeletedView;
import io.github.quizup.bff.infrastructure.in.api.response.NotificationView;
import io.github.quizup.notification.domain.event.NotificationEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Diffuse les notifications personnelles sur {@code /topic/notifications/{userId}} : c'est le
 * canal de l'inbox (invitations de défi, follows, appariement prêt). Les canaux par entité
 * ({@code /topic/lobbies/{id}}, {@code /topic/games/{id}}) restent gérés par leurs publishers.
 *
 * <p>La création pousse le {@link NotificationView} (type porté par {@code eventType}) ; la
 * suppression pousse un {@link NotificationDeletedView} sous {@code NOTIFICATION_DELETED} pour
 * que les autres onglets retirent la ligne.</p>
 */
@Service
@ProcessingGroup("notification-push")
public class NotificationPushPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPushPublisher.class);
    private static final String DESTINATION_PREFIX = "/topic/notifications/";
    private static final String DELETED_EVENT_TYPE = "NOTIFICATION_DELETED";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationPushPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void onNotificationEvent(EventMessage<?> eventMessage) {
        Object payload = eventMessage.getPayload();
        boolean created = payload instanceof NotificationEvent.NotificationCreatedEvent;
        boolean deleted = payload instanceof NotificationEvent.NotificationDeletedEvent;
        if (!created && !deleted) {
            return;
        }
        if (!(eventMessage instanceof DomainEventMessage<?> domainMessage)) {
            logger.warn("Notification ignorée (sans métadonnées d'agrégat): {}",
                    payload.getClass().getSimpleName());
            return;
        }

        EventEnvelopeResponse envelope;
        String userId;
        if (payload instanceof NotificationEvent.NotificationCreatedEvent event) {
            envelope = EventEnvelopeResponse.of(
                    event.notificationId(),
                    domainMessage.getSequenceNumber(),
                    domainMessage.getTimestamp(),
                    event.type().name(),
                    NotificationView.of(event));
            userId = event.userId();
        } else {
            NotificationEvent.NotificationDeletedEvent event =
                    (NotificationEvent.NotificationDeletedEvent) payload;
            envelope = EventEnvelopeResponse.of(
                    event.notificationId(),
                    domainMessage.getSequenceNumber(),
                    domainMessage.getTimestamp(),
                    DELETED_EVENT_TYPE,
                    new NotificationDeletedView(event.notificationId()));
            userId = event.userId();
        }
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + userId, envelope);
    }
}
