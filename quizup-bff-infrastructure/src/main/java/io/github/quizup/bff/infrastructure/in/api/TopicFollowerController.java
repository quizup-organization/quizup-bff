package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.bff.infrastructure.in.api.request.FollowTopicRequest;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.social.domain.command.TopicFollowerCommand;
import io.github.quizup.social.domain.model.FollowerIds;
import io.github.quizup.social.domain.model.TopicFollower;
import io.github.quizup.social.domain.query.TopicFollowerQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/topic-follows} — abonnement à un sujet (id déterministe {@code userId:topicId}).
 */
@RestController
@RequestMapping("/api/topic-follows")
public class TopicFollowerController {

    private static final String ENDPOINT = "/api/topic-follows";

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public TopicFollowerController(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<TopicFollower>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return queryGateway
                .query(
                        new TopicFollowerQuery.SearchTopicFollowerQuery(criteria.filters(), criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(TopicFollower.class)
                )
                .thenApply(PageMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{followId}")
    public CompletableFuture<ResponseEntity<TopicFollower>> getById(@PathVariable String followId) {
        return queryGateway
                .query(new TopicFollowerQuery.GetTopicFollowerByIdQuery(followId), QueryResponseTypes.instanceOf(TopicFollower.class))
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> follow(@RequestBody FollowTopicRequest request) {
        String userId = SecurityHelper.getUserId();
        String followId = FollowerIds.topic(request.topicId(), userId);
        return commandGateway
                .send(new TopicFollowerCommand.FollowTopicCommand(followId, request.topicId(), userId))
                .thenApply(_ -> ResponseEntityBuilder.creation(ENDPOINT, followId));
    }

    @DeleteMapping("/{followId}")
    public CompletableFuture<ResponseEntity<Void>> unfollow(@PathVariable String followId) {
        return commandGateway
                .send(new TopicFollowerCommand.UnfollowTopicCommand(followId))
                .thenApply(_ -> ResponseEntityBuilder.noContent());
    }
}
