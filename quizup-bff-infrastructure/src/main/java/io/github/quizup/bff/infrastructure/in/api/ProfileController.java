package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.query.ProfileQuery;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/profiles} — profils publics et progression, via le bus de requêtes distribué.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final QueryGateway queryGateway;

    public ProfileController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<Profile>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return queryGateway
                .query(
                        new ProfileQuery.ProfileSearchQuery(criteria.filters(), criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(Profile.class)
                )
                .thenApply(PageMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<Profile>> getById(@PathVariable String userId) {
        return queryGateway
                .query(new ProfileQuery.GetProfileQuery(userId), QueryResponseTypes.instanceOf(Profile.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}/progress")
    public CompletableFuture<ResponseEntity<PlayerProgress>> getProgress(@PathVariable String userId) {
        return queryGateway
                .query(new ProgressionQuery.GetProgressionQuery(userId), QueryResponseTypes.instanceOf(PlayerProgress.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}/progress/{topicId}")
    public CompletableFuture<ResponseEntity<TopicProgress>> getTopicProgress(
            @PathVariable String userId,
            @PathVariable String topicId) {
        return queryGateway
                .query(
                        new ProgressionQuery.GetTopicProgressionQuery(userId, topicId),
                        QueryResponseTypes.instanceOf(TopicProgress.class)
                )
                .thenApply(ResponseEntity::ok);
    }
}
