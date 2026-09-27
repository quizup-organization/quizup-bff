package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.infrastructure.in.api.response.EventEnvelopeResponse;
import io.github.quizup.bff.infrastructure.out.messaging.mapper.TicketEventNotificationMapper;
import io.github.quizup.bff.infrastructure.out.messaging.response.TicketNotification;
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
 * Diffuse les événements de matchmaking sur {@code /topic/matchmaking/tickets/{ticketId}}.
 */
@Service
@ProcessingGroup("lobby-notification")
public class TicketNotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TicketNotificationPublisher.class);
    private static final String DESTINATION_PREFIX = "/topic/matchmaking/tickets/";

    private final SimpMessagingTemplate messagingTemplate;

    public TicketNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void onLobbyEvent(EventMessage<?> eventMessage) {
        if (!(eventMessage.getPayload() instanceof LobbyEvent event)) {
            return;
        }
        TicketNotification notification = TicketEventNotificationMapper.toNotification(event);
        if (notification == null) {
            return;
        }
        send(event, eventMessage, notification);
    }

    private void send(LobbyEvent event, EventMessage<?> eventMessage, TicketNotification payload) {
        if (!(eventMessage instanceof DomainEventMessage<?> domainMessage)) {
            logger.warn("Notification ignorée (événement sans métadonnées d'agrégat): {}", event.getClass().getSimpleName());
            return;
        }

        EventEnvelopeResponse envelope = EventEnvelopeResponse.of(
                domainMessage.getAggregateIdentifier(),
                domainMessage.getSequenceNumber(),
                domainMessage.getTimestamp(),
                payload.type().name(),
                payload
        );

        logger.debug("{} publié: ticketId={}, seq={}", payload.type(), event.lobbyId(), domainMessage.getSequenceNumber());
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + event.lobbyId(), envelope);
    }
}
