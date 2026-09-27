package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ProfileViewService;
import io.github.quizup.bff.infrastructure.in.api.request.PageParams;
import io.github.quizup.bff.infrastructure.in.api.request.PeopleSort;
import io.github.quizup.bff.infrastructure.in.api.request.UpdateProfileRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ActivityViewResponse;
import io.github.quizup.bff.infrastructure.in.api.response.GameHistoryItemView;
import io.github.quizup.bff.infrastructure.in.api.response.HeadToHeadView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerCardView;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerProfileView;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.social.domain.command.UserFollowerCommand;
import io.github.quizup.social.domain.model.FollowDirection;
import io.github.quizup.social.domain.model.FollowerIds;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/profiles} — fiche joueur, listes de personnes, historique, suivi, activité.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProfileViewService profileViewService;
    private final CommandGateway commandGateway;

    public ProfileController(ProfileViewService profileViewService, CommandGateway commandGateway) {
        this.profileViewService = profileViewService;
        this.commandGateway = commandGateway;
    }

    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<PlayerProfileView>> profile(@PathVariable String userId) {
        return profileViewService
                .profileView(SecurityHelper.getUserId(), userId)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{userId}")
    public CompletableFuture<ResponseEntity<Void>> update(@PathVariable String userId,
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        return commandGateway
                .send(new ProfileCommand.UpdateProfileCommand(
                        userId,
                        SecurityHelper.getUserId(),
                        request.displayName(),
                        request.bio(),
                        request.country(),
                        request.avatarOptions()))
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @GetMapping("/{userId}/following")
    public CompletableFuture<ResponseEntity<PageResponse<PlayerCardView>>> following(
            @PathVariable String userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "RECENT") PeopleSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return people(userId, FollowDirection.FOLLOWING, q, sort, page, size);
    }

    @GetMapping("/{userId}/followers")
    public CompletableFuture<ResponseEntity<PageResponse<PlayerCardView>>> followers(
            @PathVariable String userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "RECENT") PeopleSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return people(userId, FollowDirection.FOLLOWERS, q, sort, page, size);
    }

    @PutMapping("/{userId}/follow")
    public CompletableFuture<ResponseEntity<Void>> follow(@PathVariable String userId) {
        String viewerId = SecurityHelper.getUserId();
        return commandGateway
                .send(new UserFollowerCommand.FollowUserCommand(FollowerIds.user(viewerId, userId), viewerId, userId))
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{userId}/follow")
    public CompletableFuture<ResponseEntity<Void>> unfollow(@PathVariable String userId) {
        String viewerId = SecurityHelper.getUserId();
        return commandGateway
                .send(new UserFollowerCommand.UnfollowUserCommand(FollowerIds.user(viewerId, userId), viewerId))
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{userId}/games")
    public CompletableFuture<ResponseEntity<PageResponse<GameHistoryItemView>>> games(
            @PathVariable String userId,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String opponentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return profileViewService.games(userId, topicId, opponentId, page, size).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}/head-to-head")
    public CompletableFuture<ResponseEntity<HeadToHeadView>> headToHead(
            @PathVariable String userId,
            @RequestParam("against") String against) {
        return profileViewService.headToHead(userId, against).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{userId}/activity")
    public CompletableFuture<ResponseEntity<ActivityViewResponse>> activity(
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return profileViewService.activity(userId, from, to).thenApply(ResponseEntity::ok);
    }

    private CompletableFuture<ResponseEntity<PageResponse<PlayerCardView>>> people(String userId,
                                                                                   FollowDirection direction,
                                                                                   String q,
                                                                                   PeopleSort sort,
                                                                                   int page,
                                                                                   int size) {
        return profileViewService
                .people(SecurityHelper.getUserId(), userId, direction, q, sort, page, size)
                .thenApply(ResponseEntity::ok);
    }
}
