package io.github.quizup.bff.application;

import io.github.quizup.notification.domain.event.NotificationEvent;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.profile.domain.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Envoie les notifications personnelles en Web Push, en complément du fan-out STOMP : le client
 * reçoit l'événement même applicatif fermé. Le statut {@code GONE} purge les abonnements révoqués.
 *
 * <p>Appelé hors du thread des tracking processors (executor dédié) : le profil de l'acteur est
 * résolu avec un timeout court via {@link ProfileLookup}, l'échec d'un envoi n'est jamais propagé.</p>
 */
@Service
public class WebPushDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(WebPushDispatcher.class);
    private static final long PROFILE_TIMEOUT_SECONDS = 1;

    private final PushSubscriptionService subscriptionService;
    private final WebPushGateway pushGateway;
    private final ProfileLookup profiles;

    public WebPushDispatcher(PushSubscriptionService subscriptionService,
                             WebPushGateway pushGateway,
                             ProfileLookup profiles) {
        this.subscriptionService = subscriptionService;
        this.pushGateway = pushGateway;
        this.profiles = profiles;
    }

    public void dispatch(NotificationEvent.NotificationCreatedEvent event) {
        try {
            List<PushSubscription> subscriptions = subscriptionService.subscriptionsOf(event.userId());
            if (subscriptions.isEmpty()) {
                return;
            }
            WebPushMessage message = toMessage(event);
            for (PushSubscription subscription : subscriptions) {
                WebPushGateway.SendStatus status = pushGateway.send(subscription, message);
                if (status == WebPushGateway.SendStatus.GONE) {
                    subscriptionService.unregister(event.userId(), subscription.endpoint());
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Web Push ignoré pour la notification {}: {}",
                    event.notificationId(), e.toString());
        }
    }

    private WebPushMessage toMessage(NotificationEvent.NotificationCreatedEvent event) {
        return new WebPushMessage(
                event.notificationId(),
                event.type().name(),
                event.actorId(),
                resolvePseudonym(event.actorId()),
                event.sourceId(),
                event.topicId(),
                event.gameId(),
                event.expiresAt(),
                pathFor(event));
    }

    /** Route applicative canonique du clic : chaque client la mappe (web : URL, mobile : deep link). */
    private static String pathFor(NotificationEvent.NotificationCreatedEvent event) {
        return switch (event.type()) {
            case LOBBY_INVITATION ->
                    event.sourceId() != null ? "/lobbies/" + event.sourceId() : "/notifications";
            // Partie créée (gameId attaché) : arène ; sinon salle d'attente encore ouverte.
            case LOBBY_ACCEPTED -> event.gameId() != null
                    ? "/duel/" + event.gameId()
                    : (event.sourceId() != null ? "/lobbies/" + event.sourceId() : "/notifications");
            case FOLLOW -> event.actorId() != null ? "/players/" + event.actorId() : "/notifications";
            default -> "/notifications";
        };
    }

    private String resolvePseudonym(String actorId) {
        if (actorId == null) {
            return null;
        }
        try {
            Profile profile = profiles.get(actorId).get(PROFILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return profile == null ? null : profile.pseudonym();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception _) {
            return null;
        }
    }
}
