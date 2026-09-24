package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.infrastructure.out.messaging.mapper.LobbyEventNotificationMapper;
import io.github.quizup.bff.infrastructure.out.messaging.response.LobbyNotification;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Diffuse les événements de lobby sur {@code /topic/lobbies/{lobbyId}}.
 */
@Service
@ProcessingGroup("lobby-notification")
public class LobbyNotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LobbyNotificationPublisher.class);
    private static final String DESTINATION_PREFIX = "/topic/lobbies/";

    private final SimpMessagingTemplate messagingTemplate;

    public LobbyNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void onLobbyEvent(EventMessage<?> eventMessage) {
        if (!(eventMessage.getPayload() instanceof LobbyEvent event)) {
            return;
        }
        LobbyEventNotificationMapper.toNotification(event)
                .ifPresentOrElse(
                        notification -> send(event, eventMessage, notification),
                        () -> logger.warn("Aucun mapping de notification pour l'événement: {}", event.getClass().getSimpleName()));
    }

    private void send(LobbyEvent event, EventMessage<?> eventMessage, LobbyNotification payload) {
        if (!(eventMessage instanceof DomainEventMessage<?> domainMessage)) {
            logger.warn("Notification ignorée (événement sans métadonnées d'agrégat): {}", event.getClass().getSimpleName());
            return;
        }

        NotificationEnvelope<LobbyNotification> envelope = new NotificationEnvelope<>(
                domainMessage.getIdentifier(),
                event.lobbyId(),
                domainMessage.getSequenceNumber(),
                domainMessage.getTimestamp(),
                payload
        );

        logger.debug("{} publié: lobbyId={}, seq={}", payload.type(), event.lobbyId(), domainMessage.getSequenceNumber());
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + event.lobbyId(), envelope);
    }
}
