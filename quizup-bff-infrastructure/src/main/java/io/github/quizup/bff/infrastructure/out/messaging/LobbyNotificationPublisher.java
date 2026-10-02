package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.LobbyEventNotificationMapper;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Diffuse les notifications de salon privé sur {@code /topic/lobbies/{lobbyId}}.
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
        if (!(eventMessage instanceof DomainEventMessage<?> domainMessage)) {
            logger.warn("Notification ignorée (sans métadonnées d'agrégat): {}", event.getClass().getSimpleName());
            return;
        }

        LobbyEventNotificationMapper.toNotification(event).ifPresent(notification -> {
            EventEnvelopeResponse envelope = EventEnvelopeResponse.of(
                    event.lobbyId(),
                    domainMessage.getSequenceNumber(),
                    domainMessage.getTimestamp(),
                    notification.type().name(),
                    notification);
            messagingTemplate.convertAndSend(DESTINATION_PREFIX + event.lobbyId(), envelope);
        });
    }
}
