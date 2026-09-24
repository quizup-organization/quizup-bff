package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.bff.infrastructure.in.api.request.FollowUserRequest;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.social.domain.command.UserFollowerCommand;
import io.github.quizup.social.domain.model.FollowerIds;
import io.github.quizup.social.domain.model.UserFollower;
import io.github.quizup.social.domain.query.UserFollowerQuery;
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
 * Ressource {@code /api/user-follows} — abonnement à un joueur (id déterministe {@code followerId:followedId}).
 */
@RestController
@RequestMapping("/api/user-follows")
public class UserFollowerController {

    private static final String ENDPOINT = "/api/user-follows";

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public UserFollowerController(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<UserFollower>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return queryGateway
                .query(
                        new UserFollowerQuery.SearchUserFollowerQuery(criteria.filters(), criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(UserFollower.class)
                )
                .thenApply(PageMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{followId}")
    public CompletableFuture<ResponseEntity<UserFollower>> getById(@PathVariable String followId) {
        return queryGateway
                .query(new UserFollowerQuery.GetUserFollowerByIdQuery(followId), QueryResponseTypes.instanceOf(UserFollower.class))
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> follow(@RequestBody FollowUserRequest request) {
        String followerId = SecurityHelper.getUserId();
        String followId = FollowerIds.user(followerId, request.followedId());
        return commandGateway
                .send(new UserFollowerCommand.FollowUserCommand(followId, followerId, request.followedId()))
                .thenApply(_ -> ResponseEntityBuilder.creation(ENDPOINT, followId));
    }

    @DeleteMapping("/{followId}")
    public CompletableFuture<ResponseEntity<Void>> unfollow(@PathVariable String followId) {
        return commandGateway
                .send(new UserFollowerCommand.UnfollowUserCommand(followId))
                .thenApply(_ -> ResponseEntityBuilder.noContent());
    }
}
