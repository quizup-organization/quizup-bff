package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ProfileLookup;
import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
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
    public CompletableFuture<ResponseEntity<PageResponse<Profile>>> followers(
            @PathVariable String userId,
            @RequestBody(required = false) SearchRequest searchRequest) {
        // Filtre sur `followedId = userId` ; chaque ligne porte le `followerId` à résoudre.
        return searchProfiles(userId, searchRequest, "followedId", "followerId");
    }

    @PostMapping("/{userId}/following/search")
    public CompletableFuture<ResponseEntity<PageResponse<Profile>>> following(
            @PathVariable String userId,
            @RequestBody(required = false) SearchRequest searchRequest) {
        // Filtre sur `followerId = userId` ; chaque ligne porte le `followedId` à résoudre.
        return searchProfiles(userId, searchRequest, "followerId", "followedId");
    }

    private CompletableFuture<ResponseEntity<PageResponse<Profile>>> searchProfiles(
            String userId,
            SearchRequest searchRequest,
            String filterProperty,
            String profileIdField) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        List<FilterCriteria> filters = new ArrayList<>(criteria.filters());
        filters.add(new FilterRequest(filterProperty, FilterOperator.EQUALS, userId, null, null));

        return queryGateway
                .query(
                        new UserFollowerQuery.SearchUserFollowerQuery(filters, criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(UserFollower.class)
                )
                .thenCompose(page -> resolveProfiles(page, profileIdField))
                .thenApply(ResponseEntity::ok);
    }

    private CompletableFuture<PageResponse<Profile>> resolveProfiles(
            PageResult<UserFollower> page,
            String profileIdField) {
        // Le transport du query bus ne préserve pas le type des éléments de `PageResult<T>`
        // (désérialisés en Map) : on lit le champ par nom plutôt qu'un accesseur typé.
        List<String> ids = page.content().stream()
                .map(row -> (String) ((Map<?, ?>) (Object) row).get(profileIdField))
                .toList();
        return profileLookup
                .getAll(ids)
                .thenApply(profiles -> PageMapper.toResponse(page, profiles));
    }
}
