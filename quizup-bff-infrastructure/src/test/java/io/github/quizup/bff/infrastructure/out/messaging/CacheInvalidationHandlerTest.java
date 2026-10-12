package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.application.FollowLookup;
import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.social.domain.event.UserFollowerEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Invalidations cross-réplicas : chaque pod BFF purge son cache Caffeine local sur les événements
 * social (follows) et profile (éditions) — l'invalidation locale de la réplique qui a traité
 * l'écriture n'est qu'un fast-path.
 */
class CacheInvalidationHandlerTest {

    private final FollowLookup followLookup = mock(FollowLookup.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final CacheInvalidationHandler handler =
            new CacheInvalidationHandler(followLookup, profileLookup);

    @Test
    void followedEventInvalidatesBothSides() {
        handler.on(new UserFollowerEvent.UserFollowedEvent(
                "f1:f2", "f1", "f2", Instant.now()));

        verify(followLookup).invalidateCounts("f1");
        verify(followLookup).invalidateCounts("f2");
    }

    @Test
    void unfollowedEventInvalidatesBothSides() {
        handler.on(new UserFollowerEvent.UserUnfollowedEvent(
                "f1:f2", "f1", "f2", Instant.now()));

        verify(followLookup).invalidateCounts("f1");
        verify(followLookup).invalidateCounts("f2");
    }

    @Test
    void profileFieldEventsInvalidateProfileCache() {
        Instant now = Instant.now();
        handler.on(new ProfileEvent.ProfilePseudonymUpdatedEvent("u1", "u1", "Alicia", now));
        handler.on(new ProfileEvent.ProfileBioUpdatedEvent("u1", "u1", "bio", now));
        handler.on(new ProfileEvent.ProfileCountryUpdatedEvent("u1", "u1", "FR", now));
        handler.on(new ProfileEvent.ProfileAvatarUpdatedEvent("u1", "u1", "{}", now));
        handler.on(new ProfileEvent.ProfileLanguageUpdatedEvent("u1", "u1", Language.FR, now));

        verify(profileLookup, times(5)).invalidate("u1");
    }
}
