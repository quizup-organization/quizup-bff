package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.MeView;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.social.domain.model.UserFollowCounts;
import io.github.quizup.social.domain.query.ChallengeQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Vue du joueur courant : profil, progression, compteurs d'abonnements et défis en attente.
 */
@Service
public class MeService {

    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;
    private final FollowLookup followLookup;

    public MeService(QueryGateway queryGateway, ProfileLookup profileLookup, FollowLookup followLookup) {
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
        this.followLookup = followLookup;
    }

    public CompletableFuture<MeView> me(String userId) {
        CompletableFuture<Profile> profileFuture = profileLookup.get(userId);
        CompletableFuture<PlayerProgress> progressFuture = queryGateway.query(
                new ProgressionQuery.GetProgressionQuery(userId),
                QueryResponseTypes.instanceOf(PlayerProgress.class));
        CompletableFuture<UserFollowCounts> countsFuture = followLookup.userCounts(userId);
        CompletableFuture<Long> pendingFuture = queryGateway.query(
                new ChallengeQuery.CountPendingChallengesQuery(userId),
                QueryResponseTypes.instanceOf(Long.class));

        return CompletableFuture.allOf(profileFuture, progressFuture, countsFuture, pendingFuture)
                .thenApply(_ -> {
                    Profile profile = profileFuture.join();
                    UserFollowCounts counts = countsFuture.join();
                    return new MeView(
                            profile.userId(),
                            profile.email(),
                            profile.displayName(),
                            profile.bio(),
                            profile.country(),
                            profile.avatarOptions(),
                            ProgressionViews.toView(progressFuture.join()),
                            ProgressionViews.toStats(progressFuture.join()),
                            counts.following(),
                            counts.followers(),
                            pendingFuture.join());
                });
    }
}
