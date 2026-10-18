package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.bff.infrastructure.in.api.request.ReportUserRequest;
import io.github.quizup.bff.infrastructure.in.api.request.ResolveUserReportsRequest;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserReportView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserReportResolutionView;
import io.github.quizup.bff.infrastructure.in.api.response.UserReportAckView;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.identity.domain.command.UserReportCommand;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.UserReport;
import io.github.quizup.identity.domain.model.UserReportContext;
import io.github.quizup.identity.domain.model.UserReportResolution;
import io.github.quizup.identity.domain.model.UserReportResult;
import io.github.quizup.identity.domain.model.UserReportRules;
import io.github.quizup.identity.domain.model.UserReportSnapshot;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.query.UserQuery;
import io.github.quizup.identity.domain.query.UserReportQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Signalements de joueurs : dépôt par un joueur, lecture et résolution par l'administration.
 */
@Service
public class UserReportService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public UserReportService(QueryGateway queryGateway,
                             CommandGateway commandGateway,
                             ProfileLookup profileLookup) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    public CompletableFuture<UserReportAckView> report(String targetUserId,
                                                       String reporterId,
                                                       ReportUserRequest request) {
        if (Objects.equals(targetUserId, reporterId)) {
            throw new BffProblems.UserReportNotAllowedProblem(
                    targetUserId, "Un joueur ne peut pas se signaler lui-même");
        }
        if (QuizUpConstants.SYSTEM_USER_ID.equals(targetUserId)) {
            throw new BffProblems.ProtectedAccountProblem(targetUserId);
        }
        Instant reportedAt = Instant.now();
        CompletableFuture<Boolean> targetExistsFuture = queryGateway.query(
                new UserQuery.UserExistsByIdQuery(targetUserId),
                QueryResponseTypes.instanceOf(Boolean.class));
        CompletableFuture<Game> gameFuture = request.gameId() == null || request.gameId().isBlank()
                ? CompletableFuture.completedFuture(null)
                : queryGateway.query(new GameQuery.GetGameByIdQuery(request.gameId()),
                        QueryResponseTypes.instanceOf(Game.class));
        CompletableFuture<Profile> profileFuture = profileLookup.get(targetUserId)
                .exceptionally(_ -> null);

        return CompletableFuture.allOf(targetExistsFuture, gameFuture, profileFuture).thenCompose(_ -> {
            if (!Boolean.TRUE.equals(targetExistsFuture.join())) {
                throw new UserProblems.UserNotFoundProblem(targetUserId);
            }
            Game game = gameFuture.join();
            validateGame(game, targetUserId, reporterId);

            Profile profile = profileFuture.join();
            UserReportSnapshot snapshot = profile == null
                    ? null
                    : new UserReportSnapshot(profile.pseudonym(), profile.avatarOptions());
            UserReportContext context = game == null ? UserReportContext.PROFILE : UserReportContext.GAME;
            String gameId = game == null ? null : game.gameId();
            String threadId = UserReportRules.threadId(targetUserId, reporterId);

            return commandGateway.send(new UserReportCommand.ReportUserCommand(
                            threadId,
                            targetUserId,
                            reporterId,
                            request.reasons(),
                            normalizeComment(request.comment()),
                            context,
                            gameId,
                            snapshot,
                            reportedAt))
                    .thenApply(result -> (UserReportResult) result)
                    .thenApply(result -> new UserReportAckView(
                            result.reportId(),
                            UserReportStatus.OPEN,
                            reportedAt,
                            result.created()));
        });
    }

    public CompletableFuture<List<AdminUserReportView>> adminReports(String targetUserId) {
        return reportsOf(targetUserId).thenCompose(reports -> {
            if (reports.isEmpty()) {
                return CompletableFuture.completedFuture(List.of());
            }
            List<String> reporterIds = reports.stream()
                    .map(UserReport::reporterId)
                    .distinct()
                    .toList();
            return profileLookup.getAll(reporterIds).thenApply(profiles -> {
                Map<String, String> pseudonyms = profiles.stream()
                        .collect(Collectors.toMap(
                                Profile::userId,
                                Profile::pseudonym,
                                (first, _) -> first));
                return reports.stream()
                        .map(report -> AdminUserReportView.of(report, pseudonyms.get(report.reporterId())))
                        .toList();
            });
        });
    }

    public CompletableFuture<AdminUserReportResolutionView> resolveAdmin(String reportId,
                                                                         String adminId,
                                                                         ResolveUserReportsRequest request) {
        return requireReport(reportId)
                .thenCompose(report -> resolveOne(report, adminId, request));
    }

    public CompletableFuture<AdminUserReportResolutionView> resolveAdminAll(String targetUserId,
                                                                            String adminId,
                                                                            ResolveUserReportsRequest request) {
        return openReportsOf(targetUserId)
                .thenCompose(reports -> resolveAll(reports, adminId, request));
    }

    public CompletableFuture<AdminUserReportResolutionView> resolveOpenReports(String targetUserId,
                                                                               String adminId,
                                                                               String note) {
        return openReportsOf(targetUserId).thenCompose(reports -> resolveAll(
                reports,
                adminId,
                new ResolveUserReportsRequest(UserReportResolution.RESOLVED, note)));
    }

    private CompletableFuture<AdminUserReportResolutionView> resolveOne(UserReport report,
                                                                        String reviewerId,
                                                                        ResolveUserReportsRequest request) {
        if (!report.open()) {
            return CompletableFuture.completedFuture(new AdminUserReportResolutionView(0));
        }
        return commandGateway.send(new UserReportCommand.ResolveUserReportCommand(
                        report.threadId(),
                        report.reportId(),
                        request.resolution(),
                        reviewerId,
                        request.note(),
                        Instant.now()))
                .thenApply(_ -> new AdminUserReportResolutionView(1));
    }

    private CompletableFuture<AdminUserReportResolutionView> resolveAll(List<UserReport> reports,
                                                                        String reviewerId,
                                                                        ResolveUserReportsRequest request) {
        if (reports.isEmpty()) {
            return CompletableFuture.completedFuture(new AdminUserReportResolutionView(0));
        }
        List<CompletableFuture<Void>> commands = reports.stream()
                .filter(UserReport::open)
                .map(report -> commandGateway.send(new UserReportCommand.ResolveUserReportCommand(
                                report.threadId(),
                                report.reportId(),
                                request.resolution(),
                                reviewerId,
                                request.note(),
                                Instant.now()))
                        .thenAccept(_ -> {
                        }))
                .toList();
        return CompletableFuture.allOf(commands.toArray(CompletableFuture[]::new))
                .thenApply(_ -> new AdminUserReportResolutionView(reports.size()));
    }

    private CompletableFuture<List<UserReport>> reportsOf(String targetUserId) {
        return queryGateway.query(
                new UserReportQuery.GetUserReportsByTargetQuery(targetUserId),
                QueryResponseTypes.multipleInstancesOf(UserReport.class));
    }

    private CompletableFuture<List<UserReport>> openReportsOf(String targetUserId) {
        return reportsOf(targetUserId).thenApply(reports -> reports.stream()
                .filter(UserReport::open)
                .toList());
    }

    private CompletableFuture<UserReport> requireReport(String reportId) {
        return queryGateway.query(
                new UserReportQuery.GetUserReportByIdQuery(reportId),
                QueryResponseTypes.instanceOf(UserReport.class));
    }

    private static void validateGame(Game game, String targetUserId, String reporterId) {
        if (game == null) {
            return;
        }
        if (game.status() != GameStatus.FINISHED) {
            throw new BffProblems.UserReportNotAllowedProblem(
                    targetUserId, "La partie n'est pas terminée");
        }
        boolean participant = Objects.equals(reporterId, game.player1Id())
                || Objects.equals(reporterId, game.player2Id());
        if (!participant) {
            throw new BffProblems.UserReportNotAllowedProblem(
                    targetUserId, "Le joueur n'a pas participé à cette partie");
        }
        boolean targetIsOpponent = (Objects.equals(reporterId, game.player1Id())
                && Objects.equals(targetUserId, game.player2Id()))
                || (Objects.equals(reporterId, game.player2Id())
                && Objects.equals(targetUserId, game.player1Id()));
        if (!targetIsOpponent) {
            throw new BffProblems.UserReportNotAllowedProblem(
                    targetUserId, "Le joueur signalé n'est pas l'adversaire de cette partie");
        }
    }

    private static String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }
}
