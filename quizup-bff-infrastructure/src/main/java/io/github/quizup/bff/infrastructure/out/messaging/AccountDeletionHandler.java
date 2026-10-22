package io.github.quizup.bff.infrastructure.out.messaging;

import io.github.quizup.bff.application.FollowLookup;
import io.github.quizup.bff.application.IdentityLookup;
import io.github.quizup.bff.application.MediaService;
import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.bff.application.ProgressionLookup;
import io.github.quizup.bff.application.PushSubscriptionRepository;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/** Efface les abonnements push et les médias personnels d'un compte supprimé. */
@Component
@ProcessingGroup("account-deletion-bff")
public class AccountDeletionHandler {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final MediaService mediaService;
    private final ProfileLookup profileLookup;
    private final ProgressionLookup progressionLookup;
    private final FollowLookup followLookup;
    private final IdentityLookup identityLookup;

    public AccountDeletionHandler(PushSubscriptionRepository pushSubscriptionRepository,
                                  MediaService mediaService,
                                  ProfileLookup profileLookup,
                                  ProgressionLookup progressionLookup,
                                  FollowLookup followLookup,
                                  IdentityLookup identityLookup) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.mediaService = mediaService;
        this.profileLookup = profileLookup;
        this.progressionLookup = progressionLookup;
        this.followLookup = followLookup;
        this.identityLookup = identityLookup;
    }

    @EventHandler
    public void on(UserAccountDeletedEvent event) {
        String userId = event.userId();
        pushSubscriptionRepository.deleteByUserId(userId);
        mediaService.deleteAllForUser(userId);
        profileLookup.invalidate(userId);
        progressionLookup.invalidate(userId);
        followLookup.invalidateAllCounts();
        identityLookup.invalidate(userId);
    }
}
