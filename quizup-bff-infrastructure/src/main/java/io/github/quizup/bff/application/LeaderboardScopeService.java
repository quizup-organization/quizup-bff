package io.github.quizup.bff.application;

import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.social.domain.model.UserFollower;
import io.github.quizup.social.domain.query.UserFollowerQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Résout la portée d'un classement (monde / abonnements / pays) côté BFF : le service
 * leaderboard reçoit directement les {@code memberIds} / {@code country} déjà résolus.
 */
@Service
public class LeaderboardScopeService {

    private static final int FOLLOWING_LIMIT = 200;

    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;

    public LeaderboardScopeService(QueryGateway queryGateway, ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
    }

    public ScopeFilter resolve(String scope, String requesterId) {
        if (scope == null || requesterId == null || "world".equals(scope)) {
            return ScopeFilter.world();
        }

        if ("following".equals(scope) || "friends".equals(scope)) {
            return new ScopeFilter(followingIds(requesterId), null);
        }

        if ("country".equals(scope)) {
            return new ScopeFilter(null, countryOf(requesterId));
        }

        return ScopeFilter.world();
    }

    private List<String> followingIds(String requesterId) {
        FilterRequest filter = new FilterRequest("followerId", FilterOperator.EQUALS, requesterId, null, null);
        PageResult<UserFollower> page = queryGateway
                .query(
                        new UserFollowerQuery.SearchUserFollowerQuery(List.of(filter), List.of(), new PageRequest(0, FOLLOWING_LIMIT)),
                        QueryResponseTypes.pageResultOf(UserFollower.class)
                )
                .join();

        Set<String> ids = new LinkedHashSet<>();
        ids.add(requesterId);
        page.content().forEach(follow -> ids.add(follow.followedId()));
        return new ArrayList<>(ids);
    }

    private String countryOf(String requesterId) {
        return profileLookup.get(requesterId).join().country();
    }

    public record ScopeFilter(List<String> memberIds, String country) {

        static ScopeFilter world() {
            return new ScopeFilter(null, null);
        }
    }
}
