package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ProfileUpdateService;
import io.github.quizup.bff.application.ProfileViewService;
import io.github.quizup.bff.infrastructure.in.api.request.PageParams;
import io.github.quizup.bff.infrastructure.in.api.request.PeopleSort;
import io.github.quizup.bff.infrastructure.in.api.request.UpdateAvatarOptionsRequest;
import io.github.quizup.bff.infrastructure.in.api.request.UpdateBioRequest;
import io.github.quizup.bff.infrastructure.in.api.request.UpdateCountryRequest;
import io.github.quizup.bff.infrastructure.in.api.request.UpdateLanguageRequest;
import io.github.quizup.bff.infrastructure.in.api.request.UpdatePseudonymRequest;
import io.github.quizup.bff.infrastructure.in.api.response.ActivityViewResponse;
import io.github.quizup.bff.infrastructure.in.api.response.GameHistoryItemView;
import io.github.quizup.bff.infrastructure.in.api.response.HeadToHeadView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerCardView;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerProfileView;
import io.github.quizup.microservice.security.SecurityHelper;
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
    private final ProfileUpdateService profileUpdateService;
    private final CommandGateway commandGateway;

    public ProfileController(ProfileViewService profileViewService,
                             ProfileUpdateService profileUpdateService,
                             CommandGateway commandGateway) {
        this.profileViewService = profileViewService;
        this.profileUpdateService = profileUpdateService;
        this.commandGateway = commandGateway;
    }

    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<PlayerProfileView>> profile(@PathVariable String userId) {
        return profileViewService
                .profileView(SecurityHelper.getUserId(), userId)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{userId}/pseudonym")
    public CompletableFuture<ResponseEntity<Void>> updatePseudonym(
            @PathVariable String userId,
            @Valid @RequestBody UpdatePseudonymRequest request) {
        return profileUpdateService
                .updatePseudonym(userId, SecurityHelper.getUserId(), request.pseudonym())
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{userId}/bio")
    public CompletableFuture<ResponseEntity<Void>> updateBio(
            @PathVariable String userId,
            @Valid @RequestBody UpdateBioRequest request) {
        return profileUpdateService
                .updateBio(userId, SecurityHelper.getUserId(), request.bio())
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{userId}/country")
    public CompletableFuture<ResponseEntity<Void>> updateCountry(
            @PathVariable String userId,
            @Valid @RequestBody UpdateCountryRequest request) {
        return profileUpdateService
                .updateCountry(userId, SecurityHelper.getUserId(), request.country())
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{userId}/avatar-options")
    public CompletableFuture<ResponseEntity<Void>> updateAvatarOptions(
            @PathVariable String userId,
            @Valid @RequestBody UpdateAvatarOptionsRequest request) {
        return profileUpdateService
                .updateAvatarOptions(userId, SecurityHelper.getUserId(), request.avatarOptions())
                .thenApply(_ -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{userId}/language")
    public CompletableFuture<ResponseEntity<Void>> updateLanguage(
            @PathVariable String userId,
            @Valid @RequestBody UpdateLanguageRequest request) {
        return profileUpdateService
                .updateLanguage(userId, SecurityHelper.getUserId(), request.language())
                .thenApply(_ -> ResponseEntity.noContent().build());
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
