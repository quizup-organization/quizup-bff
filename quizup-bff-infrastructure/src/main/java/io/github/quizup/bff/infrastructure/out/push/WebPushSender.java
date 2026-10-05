package io.github.quizup.bff.infrastructure.out.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.application.PushSubscription;
import io.github.quizup.bff.application.WebPushGateway;
import io.github.quizup.bff.application.WebPushMessage;
import io.github.quizup.notification.domain.model.NotificationType;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.jwt.nimbus.NimbusJwtFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;

/**
 * Envoi Web Push (RFC 8030/8291) via {@code dev.blanke.webpush} : JWT VAPID signé Nimbus,
 * chiffrement BouncyCastle, transport {@link java.net.http.HttpClient}.
 */
@Component
public class WebPushSender implements WebPushGateway {

    static {
        // La lib charge les clés VAPID via le provider BouncyCastle explicite (`BC`) : sans
        // enregistrement dans la JVM, `withVapidPublicKey` échoue au démarrage (push désactivé).
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(WebPushSender.class);
    private static final int DEFAULT_TTL_SECONDS = 43_200;
    private static final int MIN_TTL_SECONDS = 60;
    private static final int MAX_TTL_SECONDS = 86_400;

    private final ObjectMapper objectMapper;
    private final PushService pushService;

    public WebPushSender(WebPushProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (properties.configured()) {
            this.pushService = buildPushService(properties);
        } else {
            logger.info("Web Push désactivé : clés VAPID absentes (quizup.push.vapid.*)");
            this.pushService = null;
        }
    }

    @Override
    public SendStatus send(PushSubscription subscription, WebPushMessage message) {
        if (pushService == null) {
            return SendStatus.FAILED;
        }
        try {
            Notification.Builder builder = Notification.builder()
                    .endpoint(subscription.endpoint())
                    .userPublicKey(subscription.p256dh())
                    .userAuth(subscription.auth())
                    .payload(objectMapper.writeValueAsString(message))
                    .ttl(ttlSeconds(message))
                    .urgency(urgency(message));
            String topic = topic(message);
            if (topic != null) {
                builder.topic(topic);
            }

            HttpResponse<Void> response = pushService.send(builder.build());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return SendStatus.DELIVERED;
            }
            if (status == 404 || status == 410) {
                return SendStatus.GONE;
            }
            logger.warn("Web Push {} refusé (HTTP {}) pour {}", message.type(), status, subscription.endpoint());
            return SendStatus.FAILED;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return SendStatus.FAILED;
        } catch (Exception e) {
            logger.warn("Web Push {} en échec pour {}: {}", message.type(), subscription.endpoint(), e.toString());
            return SendStatus.FAILED;
        }
    }

    private static PushService buildPushService(WebPushProperties properties) {
        try {
            return PushService.builder()
                    .withVapidPublicKey(properties.vapid().publicKey())
                    .withVapidPrivateKey(properties.vapid().privateKey())
                    .withVapidSubject(properties.vapid().subject())
                    .withJwtFactory(new NimbusJwtFactory())
                    .build();
        } catch (GeneralSecurityException e) {
            logger.error("Clés VAPID invalides : Web Push désactivé", e);
            return null;
        }
    }

    private static int ttlSeconds(WebPushMessage message) {
        if (message.expiresAt() == null) {
            return DEFAULT_TTL_SECONDS;
        }
        long seconds = Duration.between(Instant.now(), message.expiresAt()).getSeconds();
        return (int) Math.max(MIN_TTL_SECONDS, Math.min(MAX_TTL_SECONDS, seconds));
    }

    private static Notification.Urgency urgency(WebPushMessage message) {
        return NotificationType.LOBBY_INVITATION.name().equals(message.type())
                ? Notification.Urgency.HIGH
                : Notification.Urgency.NORMAL;
    }

    /** Collapse des invitations d'un même salon — le header Topic est limité à 32 caractères. */
    private static String topic(WebPushMessage message) {
        if (!NotificationType.LOBBY_INVITATION.name().equals(message.type()) || message.sourceId() == null) {
            return null;
        }
        return "lobby-" + Integer.toHexString(message.sourceId().hashCode());
    }
}
