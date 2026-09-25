package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SortRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.social.domain.model.UserFollower;
import io.github.quizup.social.domain.query.UserFollowerQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Sous-collections {@code /api/profiles/{userId}/followers|following} — renvoient des
 * {@link Profile} complets (résolution des suivis en une requête, puis profils), pour supprimer
 * le fan-out côté client.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileFollowerController {

    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;

    public ProfileFollowerController(QueryGateway queryGateway, ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
    }

    @PostMapping("/{userId}/followers/search")
    public CompletableFuture<ResponseEntity<SearchResponse<Profile>>> followers(
            @PathVariable String userId,
            @RequestBody(required = false) SearchRequest searchRequest) {
        // Filtre sur `followedId = userId` ; chaque ligne porte le `followerId` à résoudre.
        return searchProfiles(userId, searchRequest, "followedId", "followerId");
    }

    @PostMapping("/{userId}/following/search")
    public CompletableFuture<ResponseEntity<SearchResponse<Profile>>> following(
            @PathVariable String userId,
            @RequestBody(required = false) SearchRequest searchRequest) {
        // Filtre sur `followerId = userId` ; chaque ligne porte le `followedId` à résoudre.
        return searchProfiles(userId, searchRequest, "followerId", "followedId");
    }

    private CompletableFuture<ResponseEntity<SearchResponse<Profile>>> searchProfiles(
            String userId,
            SearchRequest searchRequest,
            String filterProperty,
            String profileIdField) {
        List<FilterRequest> filters = new ArrayList<>();
        if (searchRequest != null && searchRequest.filters() != null) {
            filters.addAll(searchRequest.filters());
        }
        filters.add(new FilterRequest(filterProperty, FilterOperator.EQUALS, userId, null, null));

        List<SortRequest> sorts = (searchRequest == null || searchRequest.sorts() == null)
                ? List.of() : searchRequest.sorts();
        PageRequest page = searchRequest == null ? null : searchRequest.page();
        SearchRequest merged = new SearchRequest(filters, sorts, page);

        return queryGateway
                .query(new UserFollowerQuery.SearchUserFollowerQuery(merged), QueryResponseTypes.searchResponseOf(UserFollower.class))
                .thenCompose(response -> resolveProfiles(response, profileIdField))
                .thenApply(ResponseEntity::ok);
    }

    private CompletableFuture<SearchResponse<Profile>> resolveProfiles(
            SearchResponse<UserFollower> response,
            String profileIdField) {
        // Le transport du query bus peut renvoyer soit des `UserFollower` typés, soit des Map
        // (typage d'élément perdu) : on tolère les deux. On streame sur `List<?>` pour éviter le
        // cast implicite en `UserFollower` inséré par le compilateur.
        List<String> ids = ((List<?>) response.content()).stream()
                .map(row -> extractFollowedId(row, profileIdField))
                .filter(Objects::nonNull)
                .toList();
        return profileLookup
                .getAll(ids)
                .thenApply(profiles -> PageMapper.toResponse(response, profiles));
    }

    private static String extractFollowedId(Object row, String profileIdField) {
        if (row instanceof Map<?, ?> map) {
            Object value = map.get(profileIdField);
            return value == null ? null : value.toString();
        }
        if (row instanceof UserFollower follow) {
            return "followerId".equals(profileIdField) ? follow.followerId() : follow.followedId();
        }
        return null;
    }
}
