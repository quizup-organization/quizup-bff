package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.MatchmakingTicketView;
import io.github.quizup.matchmaking.domain.command.MatchmakingCommand;
import io.github.quizup.matchmaking.domain.model.Matchmaking;
import io.github.quizup.matchmaking.domain.model.MatchmakingStatus;
import io.github.quizup.matchmaking.domain.query.MatchmakingQuery;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Écritures et lecture de la recherche d'appariement public (« Défier le monde »).
 */
@Service
public class MatchmakingViewService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public MatchmakingViewService(QueryGateway queryGateway,
                                  CommandGateway commandGateway,
                                  ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    /** Entre en recherche : résout niveau et langue du joueur, puis crée l'agrégat. */
    public CompletableFuture<MatchmakingTicketView> enqueue(String playerId, String topicId) {
        CompletableFuture<Profile> profileFuture = profileLookup.get(playerId);
        CompletableFuture<PlayerProgress> progressFuture = queryGateway.query(
                new ProgressionQuery.GetProgressionQuery(playerId),
                QueryResponseTypes.instanceOf(PlayerProgress.class));

        return profileFuture.thenCombine(progressFuture, (profile, progress) -> {
            String matchmakingId = UUID.randomUUID().toString();
            Set<Language> languages = profile.language() == null
                    ? Set.of()
                    : Set.of(profile.language());
            return new PendingCommand(matchmakingId, topicId, levelOf(progress), languages);
        }).thenCompose(pending -> commandGateway
                .send(new MatchmakingCommand.CreateMatchmakingCommand(
                        pending.matchmakingId(), playerId, pending.topicId(), pending.level(), pending.languages()))
                .thenApply(_ -> new MatchmakingTicketView(
                        pending.matchmakingId(),
                        pending.topicId(),
                        MatchmakingStatus.SEARCHING,
                        null,
                        null,
                        false,
                        Instant.now(),
                        Instant.now())));
    }

    public CompletableFuture<MatchmakingTicketView> get(String ticketId) {
        return queryGateway
                .query(new MatchmakingQuery.GetMatchmakingByIdQuery(ticketId),
                        QueryResponseTypes.instanceOf(Matchmaking.class))
                .thenApply(MatchmakingViewService::toView);
    }

    public CompletableFuture<Void> cancel(String playerId, String ticketId) {
        return commandGateway
                .send(new MatchmakingCommand.CancelMatchmakingCommand(ticketId, playerId))
                .thenAccept(_ -> {
                });
    }

    private static int levelOf(PlayerProgress progress) {
        return progress == null ? 1 : Math.max(1, progress.level());
    }

    private static MatchmakingTicketView toView(Matchmaking matchmaking) {
        return new MatchmakingTicketView(
                matchmaking.matchmakingId(),
                matchmaking.topicId(),
                matchmaking.status(),
                matchmaking.gameId(),
                matchmaking.opponentId(),
                matchmaking.vsBot(),
                matchmaking.createdAt(),
                matchmaking.updatedAt());
    }

    private record PendingCommand(String matchmakingId, String topicId, int level, Set<Language> languages) {
    }
}
