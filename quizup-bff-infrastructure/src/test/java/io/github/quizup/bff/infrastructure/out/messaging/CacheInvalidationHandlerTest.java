package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.application.FollowLookup;
import io.github.quizup.bff.application.IdentityLookup;
import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.bff.application.ProgressionLookup;
import io.github.quizup.bff.application.RewardLookup;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.event.RewardEvent;
import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardSource;
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
    private final IdentityLookup identityLookup = mock(IdentityLookup.class);
    private final ProgressionLookup progressionLookup = mock(ProgressionLookup.class);
    private final RewardLookup rewardLookup = mock(RewardLookup.class);
    private final CacheInvalidationHandler handler =
            new CacheInvalidationHandler(
                    followLookup, profileLookup, identityLookup, progressionLookup, rewardLookup);

    @Test
    void accountDeletionInvalidatesAllLocalCaches() {
        handler.on(new UserAccountDeletedEvent(
                "u1", "deletion-1", Instant.now()));

        verify(profileLookup).invalidate("u1");
        verify(progressionLookup).invalidate("u1");
        verify(rewardLookup).invalidate("u1");
        verify(identityLookup).invalidate("u1");
        verify(followLookup).invalidateAllCounts();
    }

    @Test
    void rewardGrantedInvalidatesRewardsCache() {
        handler.on(new RewardEvent.RewardGrantedEvent(
                "u1", RewardCode.MICAH_AVATAR, RewardSource.BADGE, Instant.now()));

        verify(rewardLookup).invalidate("u1");
    }

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
        handler.on(new ProfileEvent.ProfileBannerUpdatedEvent("u1", "u1", "https://media.example.test/b.jpg", now));
        handler.on(new ProfileEvent.ProfileLanguageUpdatedEvent("u1", "u1", Language.FR, now));
        handler.on(new ProfileEvent.ProfileEquippedTitleUpdatedEvent("u1", "u1", "t1:NOVICE", now));

        verify(profileLookup, times(7)).invalidate("u1");
    }

    @Test
    void accountStatusEventsInvalidateIdentityCache() {
        Instant now = Instant.now();
        handler.on(new UserEvent.UserSuspendedEvent(
                "u1", "u1@quizup.dev", "s1",
                io.github.quizup.identity.domain.model.UserSuspensionReason.HARASSMENT,
                null, "admin-1", now, null));
        handler.on(new UserEvent.UserReinstatedEvent(
                "u1", "u1@quizup.dev", "s1",
                io.github.quizup.identity.domain.model.SuspensionReinstatementReason.MANUAL,
                null, "admin-1", now));

        verify(identityLookup, times(2)).invalidate("u1");
    }
}
