package io.github.quizup.bff.infrastructure.in.websocket;

import io.github.quizup.profile.domain.command.PresenceCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Le BFF est la seule surface STOMP : il traduit le cycle de vie des sessions client
 * (CONNECT/DISCONNECT) en commandes de présence envoyées à {@code quizup-profile}.
 * Le {@code Principal} est posé par le {@code StompAuthChannelInterceptor} du SDK
 * (nom = claim {@code user_id}).
 */
@Component
public class PresenceSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(PresenceSessionListener.class);

    private final CommandGateway commandGateway;

    public PresenceSessionListener(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        logger.debug("Présence: session connectée userId={}, sessionId={}", user.getName(), sessionId);
        commandGateway.send(new PresenceCommand.ConnectPlayerCommand(sessionId, user.getName()));
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        logger.debug("Présence: session déconnectée userId={}, sessionId={}", user.getName(), sessionId);
        commandGateway.send(new PresenceCommand.DisconnectPlayerCommand(sessionId, user.getName()));
    }
}
