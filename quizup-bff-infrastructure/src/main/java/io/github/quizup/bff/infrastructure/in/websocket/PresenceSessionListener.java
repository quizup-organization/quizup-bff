package io.github.quizup.bff.infrastructure.in.websocket;

import io.github.quizup.profile.domain.command.PresenceCommand;
import io.github.quizup.profile.domain.model.PresenceRules;
import jakarta.annotation.PreDestroy;
import org.axonframework.commandhandling.NoHandlerForCommandException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Le BFF est la seule surface STOMP : il traduit le cycle de vie des sessions client
 * (CONNECT/DISCONNECT) en commandes de présence envoyées à {@code quizup-profile}.
 * Le {@code Principal} est posé par le {@code StompAuthChannelInterceptor} du SDK
 * (nom = claim {@code user_id}).
 *
 * <p>Chaque instance BFF porte un identifiant stable ({@code application:host}). La présence
 * {@code quizup-profile} est adossée à des leases TTL : le BFF <b>renouvelle en batch</b> les
 * leases de ses sessions locales ({@link PresenceRules#SESSION_RENEW_INTERVAL}) — un crash
 * d'instance laisse les leases expirer, aucune purge au démarrage n'est nécessaire.</p>
 *
 * <p><b>Fiabilité</b> : au démarrage, la découverte du command bus distribué peut ne pas encore
 * connaître {@code quizup-profile}, et un envoi fire-and-forget perdrait la commande. Les
 * commandes de session sont donc retentées tant que le routage n'est pas prêt
 * ({@link NoHandlerForCommandException}), dans une fenêtre bornée ; le heartbeat réessaie
 * simplement au tick suivant.</p>
 */
@Component
public class PresenceSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(PresenceSessionListener.class);
    private static final int MAX_ROUTING_ATTEMPTS = 30;
    private static final int MAX_RENEW_ATTEMPTS = 3;
    private static final long ROUTING_RETRY_DELAY_SECONDS = 1;

    private final CommandGateway commandGateway;
    private final String instanceId;
    private final Map<String, String> localSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService renewer;

    public PresenceSessionListener(CommandGateway commandGateway,
                                   @Value("${spring.application.name:quizup-bff}") String applicationName,
                                   @Value("${HOSTNAME:localhost}") String hostname) {
        this.commandGateway = commandGateway;
        this.instanceId = applicationName + ":" + hostname;
        this.renewer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "presence-renewer");
            thread.setDaemon(true);
            return thread;
        });
        long intervalMillis = PresenceRules.SESSION_RENEW_INTERVAL.toMillis();
        this.renewer.scheduleWithFixedDelay(this::renewSessions, intervalMillis, intervalMillis,
                TimeUnit.MILLISECONDS);
        logger.info("Présence: heartbeat des leases toutes les {}s (instance {})",
                PresenceRules.SESSION_RENEW_INTERVAL.toSeconds(), instanceId);
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        registerSession(sessionId, user.getName());
        logger.debug("Présence: session connectée userId={}, sessionId={}", user.getName(), sessionId);
        sendWithRoutingRetry(new PresenceCommand.ConnectPlayerCommand(sessionId, user.getName(), instanceId),
                "connect " + sessionId, 1, MAX_ROUTING_ATTEMPTS);
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        unregisterSession(sessionId);
        logger.debug("Présence: session déconnectée userId={}, sessionId={}", user.getName(), sessionId);
        sendWithRoutingRetry(new PresenceCommand.DisconnectPlayerCommand(sessionId, user.getName()),
                "disconnect " + sessionId, 1, MAX_ROUTING_ATTEMPTS);
    }

    /** Heartbeat : renouvelle les leases de toutes les sessions locales (1 commande batch). */
    void renewSessions() {
        if (localSessions.isEmpty()) {
            return;
        }
        List<String> sessionIds = List.copyOf(localSessions.keySet());
        sendWithRoutingRetry(new PresenceCommand.RenewPresenceSessionsCommand(sessionIds),
                "renew " + sessionIds.size() + " session(s)", 1, MAX_RENEW_ATTEMPTS);
    }

    void registerSession(String sessionId, String userId) {
        localSessions.put(sessionId, userId);
    }

    void unregisterSession(String sessionId) {
        localSessions.remove(sessionId);
    }

    @PreDestroy
    public void shutdown() {
        renewer.shutdownNow();
    }

    private void sendWithRoutingRetry(Object command, String description, int attempt, int maxAttempts) {
        commandGateway.send(command).whenComplete((result, error) -> {
            if (error == null) {
                return;
            }
            if (!isRoutingNotReady(error)) {
                logger.warn("Présence: commande {} en échec", description, error);
                return;
            }
            if (attempt >= maxAttempts) {
                logger.warn("Présence: commande {} abandonnée après {} tentatives (routage indisponible)",
                        description, attempt);
                return;
            }
            CompletableFuture.delayedExecutor(ROUTING_RETRY_DELAY_SECONDS, TimeUnit.SECONDS)
                    .execute(() -> sendWithRoutingRetry(command, description, attempt + 1, maxAttempts));
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
