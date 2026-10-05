package io.github.quizup.bff.infrastructure.out.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration Web Push (clés VAPID). Propriétés : {@code quizup.push.vapid.public-key},
 * {@code quizup.push.vapid.private-key}, {@code quizup.push.vapid.subject} (mailto: ou https:).
 * Sans clés, le Web Push est silencieusement désactivé (le STOMP reste la voie nominale).
 */
@ConfigurationProperties(prefix = "quizup.push")
public record WebPushProperties(Vapid vapid) {

    public WebPushProperties {
        if (vapid == null) {
            vapid = new Vapid(null, null, null);
        }
    }

    public record Vapid(String publicKey, String privateKey, String subject) {
    }

    public boolean configured() {
        return hasText(vapid.publicKey()) && hasText(vapid.privateKey()) && hasText(vapid.subject());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
