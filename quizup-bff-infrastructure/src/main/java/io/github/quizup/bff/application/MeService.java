package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.MeView;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.social.domain.model.UserFollowCounts;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Vue du joueur courant : profil, progression, compteurs d'abonnements et défis en attente.
 * Les trois lectures passent par les lookups cachés (Caffeine) : `/api/me` est appelé sur chaque
 * écran et en polling, on amortit les requêtes distribuées (`profile`, `profile`, `social`).
 */
@Service
public class MeService {

    private final ProfileLookup profileLookup;
    private final ProgressionLookup progressionLookup;
    private final FollowLookup followLookup;
    private final PremiumAccessPolicy premiumAccessPolicy;

    public MeService(ProfileLookup profileLookup,
                     ProgressionLookup progressionLookup,
                     FollowLookup followLookup,
                     PremiumAccessPolicy premiumAccessPolicy) {
        this.profileLookup = profileLookup;
        this.progressionLookup = progressionLookup;
        this.followLookup = followLookup;
        this.premiumAccessPolicy = premiumAccessPolicy;
    }

    public CompletableFuture<MeView> me(String userId) {
        // Capturé sur le thread HTTP : le callback Axon peut s'exécuter sur un autre thread sans
        // SecurityContext, ce qui ferait perdre le rôle admin/premium.
        return me(userId, premiumAccessPolicy.isPremium());
    }

    /** Variante pour les appels déclenchés depuis un callback asynchrone (fin d'onboarding). */
    public CompletableFuture<MeView> me(String userId, boolean premium) {
        CompletableFuture<Profile> profileFuture = profileLookup.get(userId);
        CompletableFuture<PlayerProgress> progressFuture = progressionLookup.get(userId);
        CompletableFuture<UserFollowCounts> countsFuture = followLookup.userCounts(userId);

        return CompletableFuture.allOf(profileFuture, progressFuture, countsFuture)
                .thenApply(_ -> {
                    Profile profile = profileFuture.join();
                    PlayerProgress progress = progressFuture.join();
                    UserFollowCounts counts = countsFuture.join();
                    return new MeView(
                            profile.userId(),
                            profile.email(),
                            profile.pseudonym(),
                            profile.bio(),
                            profile.country(),
                            profile.avatarOptions(),
                            profile.bannerUrl(),
                            profile.equippedTitle(),
                            profile.language(),
                            ProgressionViews.toView(progress),                            ProgressionViews.toStats(progress),
                            counts.following(),
                            counts.followers(),
                            profile.onboardingCompletedAt() != null || progress.gamesPlayed() > 0,
                            premium);
                });
    }

    public boolean isPremium() {
        return premiumAccessPolicy.isPremium();
    }
}
