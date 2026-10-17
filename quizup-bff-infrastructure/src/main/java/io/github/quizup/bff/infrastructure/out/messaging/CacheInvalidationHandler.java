package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.application.FollowLookup;
import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.social.domain.event.UserFollowerEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Invalidation des caches de lecture sur <b>toutes</b> les répliques BFF : un follow/unfollow ou
 * une édition de profil n'invalide que la réplique qui a traité la commande, les autres
 * serviraient sinon une valeur périmée jusqu'au TTL (30 s pour les compteurs de follows).
 * <p>
 * Le groupe est <b>instance-scopé</b> (cf. {@code quizup.axon.instance-scoped-groups}) : chaque pod
 * consomme tous les événements et purge son propre cache Caffeine. Les lectures qui suivent sont
 * protégées par la fenêtre no-cache des lookups (la projection peut être en retard de quelques
 * millisecondes sur l'événement).
 */
@Service
@ProcessingGroup("cache-invalidation")
public class CacheInvalidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationHandler.class);

    private final FollowLookup followLookup;
    private final ProfileLookup profileLookup;

    public CacheInvalidationHandler(FollowLookup followLookup, ProfileLookup profileLookup) {
        this.followLookup = followLookup;
        this.profileLookup = profileLookup;
    }

    @EventHandler
    public void on(UserFollowerEvent.UserFollowedEvent event) {
        invalidateCounts(event);
    }

    @EventHandler
    public void on(UserFollowerEvent.UserUnfollowedEvent event) {
        invalidateCounts(event);
    }

    @EventHandler
    public void on(ProfileEvent.ProfilePseudonymUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    @EventHandler
    public void on(ProfileEvent.ProfileBioUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    @EventHandler
    public void on(ProfileEvent.ProfileCountryUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    @EventHandler
    public void on(ProfileEvent.ProfileAvatarUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    @EventHandler
    public void on(ProfileEvent.ProfileBannerUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    @EventHandler
    public void on(ProfileEvent.ProfileLanguageUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    @EventHandler
    public void on(ProfileEvent.ProfileEquippedTitleUpdatedEvent event) {
        invalidateProfile(event.userId());
    }

    private void invalidateCounts(UserFollowerEvent event) {
        // Le follower (followingCount) et le suivi (followersCount) changent tous les deux.
        followLookup.invalidateCounts(event.followerId());
        followLookup.invalidateCounts(event.followedId());
        logger.debug("Compteurs de follows invalidés: followerId={}, followedId={}",
                event.followerId(), event.followedId());
    }

    private void invalidateProfile(String userId) {
        profileLookup.invalidate(userId);
        logger.debug("Profil invalidé: userId={}", userId);
    }
}
