package io.github.quizup.bff.infrastructure.out.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.application.PushSubscription;
import io.github.quizup.bff.application.WebPushGateway;
import io.github.quizup.bff.application.WebPushMessage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WebPushSenderTest {

    /** Paire VAPID de développement (application-local.yml) — clé valide, aucun envoi réseau. */
    private static final String PUBLIC_KEY =
            "BL4_f0c1UrFgu1Hdym2XedjmA5cjTg8hDLn_AxU-MvhEf1aTauiA2Ph0_j4t89siXdF5dD4E6URhkonwHTBWxdE";
    private static final String PRIVATE_KEY = "hM9-hr1OPKWJ3lpi_bl6Hax-24ovYnB1KDORMkGvnb0";

    @Test
    void builds_push_service_with_bouncycastle_provider_registered() {
        WebPushProperties properties = new WebPushProperties(
                new WebPushProperties.Vapid(PUBLIC_KEY, PRIVATE_KEY, "mailto:dev@quizup.local"));

        assertThatCode(() -> new WebPushSender(properties, new ObjectMapper()))
                .doesNotThrowAnyException();
        assertThat(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isNotNull();
    }

    @Test
    void disabled_without_keys_fails_softly() {
        WebPushSender sender = new WebPushSender(new WebPushProperties(null), new ObjectMapper());
        PushSubscription subscription = new PushSubscription(
                "https://push.example/1", "user-1", "p256dh", "auth", "Chrome", Instant.now(), Instant.now());
        WebPushMessage message = new WebPushMessage(
                "notification-1", "LOBBY_INVITATION", "actor-1", "Alice", "lobby-1",
                "topic-1", null, null, "/lobbies/lobby-1");

        assertThat(sender.send(subscription, message)).isEqualTo(WebPushGateway.SendStatus.FAILED);
    }
}
