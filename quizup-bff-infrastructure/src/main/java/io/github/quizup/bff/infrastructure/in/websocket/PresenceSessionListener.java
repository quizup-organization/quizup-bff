package io.github.quizup.bff.infrastructure.in.websocket;

import io.github.quizup.profile.domain.command.PresenceCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.NoHandlerForCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Le BFF est la seule surface STOMP : il traduit le cycle de vie des sessions client
 * (CONNECT/DISCONNECT) en commandes de présence envoyées à {@code quizup-profile}.
 * Le {@code Principal} est posé par le {@code StompAuthChannelInterceptor} du SDK
 * (nom = claim {@code user_id}).
 *
 * <p>Chaque instance BFF porte un identifiant stable ({@code application:host}) : ses sessions
 * sont rattachées à cet identifiant, et un redémarrage purge uniquement les siennes (ouvertes
 * avant son démarrage).</p>
 *
 * <p><b>Fiabilité</b> : au démarrage, la découverte du command bus distribué peut ne pas encore
 * connaître {@code quizup-profile}, et un envoi fire-and-forget perdrait la commande. Toutes les
 * commandes de présence sont donc retentées tant que le routage n'est pas prêt
 * ({@link NoHandlerForCommandException}), dans une fenêtre bornée.</p>
 */
@Component
public class PresenceSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(PresenceSessionListener.class);
    private static final int MAX_ROUTING_ATTEMPTS = 30;
    private static final long ROUTING_RETRY_DELAY_SECONDS = 1;

    private final CommandGateway commandGateway;
    private final String instanceId;
    private final Instant startedAt = Instant.now();

    public PresenceSessionListener(CommandGateway commandGateway,
                                   @Value("${spring.application.name:quizup-bff}") String applicationName,
                                   @Value("${HOSTNAME:localhost}") String hostname) {
        this.commandGateway = commandGateway;
        this.instanceId = applicationName + ":" + hostname;
    }

    /** Purge les sessions laissées par une incarnation précédente de cette instance. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Présence: purge des sessions de l'instance {} antérieures à {}", instanceId, startedAt);
        sendWithRoutingRetry(new PresenceCommand.ResetInstanceSessionsCommand(instanceId, startedAt),
                "reset-instance " + instanceId, 1);
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        logger.debug("Présence: session connectée userId={}, sessionId={}", user.getName(), sessionId);
        sendWithRoutingRetry(new PresenceCommand.ConnectPlayerCommand(sessionId, user.getName(), instanceId),
                "connect " + sessionId, 1);
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        logger.debug("Présence: session déconnectée userId={}, sessionId={}", user.getName(), sessionId);
        sendWithRoutingRetry(new PresenceCommand.DisconnectPlayerCommand(sessionId, user.getName()),
                "disconnect " + sessionId, 1);
    }

    private void sendWithRoutingRetry(Object command, String description, int attempt) {
        commandGateway.send(command).whenComplete((result, error) -> {
            if (error == null) {
                return;
            }
            if (!isRoutingNotReady(error)) {
                logger.warn("Présence: commande {} en échec", description, error);
                return;
            }
            if (attempt >= MAX_ROUTING_ATTEMPTS) {
                logger.warn("Présence: commande {} abandonnée après {} tentatives (routage indisponible)",
                        description, attempt);
                return;
            }
            CompletableFuture.delayedExecutor(ROUTING_RETRY_DELAY_SECONDS, TimeUnit.SECONDS)
                    .execute(() -> sendWithRoutingRetry(command, description, attempt + 1));
        });
    }

    private static boolean isRoutingNotReady(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof NoHandlerForCommandException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
