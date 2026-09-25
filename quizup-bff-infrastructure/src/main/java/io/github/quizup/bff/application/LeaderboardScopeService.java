package io.github.quizup.bff.application;

import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
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
        SearchRequest request = new SearchRequest(
                List.of(new FilterRequest("followerId", FilterOperator.EQUALS, requesterId, null, null)),
                List.of(),
                new PageRequest(0, FOLLOWING_LIMIT)
        );
        SearchResponse<UserFollower> page = queryGateway
                .query(new UserFollowerQuery.SearchUserFollowerQuery(request), QueryResponseTypes.searchResponseOf(UserFollower.class))
                .join();

        Set<String> ids = new LinkedHashSet<>();
        ids.add(requesterId);
        // Le transport du query bus peut renvoyer des `UserFollower` typés (SDK récent) ou des Map
        // (typage d'élément perdu) : on tolère les deux. On itère sur `List<?>` pour éviter le
        // cast implicite en `UserFollower` inséré par le compilateur.
        ((java.util.List<?>) page.content()).forEach(row -> {
            String followedId = extractFollowedId(row);
            if (followedId != null) {
                ids.add(followedId);
            }
        });
        return new ArrayList<>(ids);
    }

    private static String extractFollowedId(Object row) {
        if (row instanceof java.util.Map<?, ?> map) {
            Object value = map.get("followedId");
            return value == null ? null : value.toString();
        }
        if (row instanceof UserFollower follow) {
            return follow.followedId();
        }
        return null;
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
