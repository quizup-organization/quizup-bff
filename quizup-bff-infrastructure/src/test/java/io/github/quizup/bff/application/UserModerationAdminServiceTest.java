package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.bff.infrastructure.in.api.request.SuspendUserRequest;
import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import io.github.quizup.identity.domain.query.UserQuery;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserModerationAdminServiceTest {

    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final IdentityLookup identityLookup = mock(IdentityLookup.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final UserReportService userReportService = mock(UserReportService.class);
    private final UserModerationAdminService service = new UserModerationAdminService(
            queryGateway, commandGateway, identityLookup, profileLookup, userReportService);

    @Test
    void suspend_temporary_sendsCommandAndResolvesReports() {
        stubUser(activeUser());
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(userReportService.resolveOpenReports(eq("user-2"), eq("admin-1"), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new io.github.quizup.bff.infrastructure.in.api.response.AdminUserReportResolutionView(2)));

        var result = service.suspend("user-2", "admin-1", new SuspendUserRequest(
                UserSuspensionReason.HARASSMENT, "internal", Duration.ofDays(7), false, true)).join();

        ArgumentCaptor<UserCommand.SuspendUserCommand> captor =
                ArgumentCaptor.forClass(UserCommand.SuspendUserCommand.class);
        org.mockito.Mockito.verify(commandGateway).send(captor.capture());
        UserCommand.SuspendUserCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo("user-2");
        assertThat(command.requestedBy()).isEqualTo("admin-1");
        assertThat(command.suspendedUntil()).isAfter(Instant.now().plus(Duration.ofDays(6)));
        assertThat(command.note()).isEqualTo("internal");
        assertThat(result.resolvedReports()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void suspend_permanent_rejectsDuration() {
        stubUser(activeUser());

        assertThatThrownBy(() -> service.suspend("user-2", "admin-1", new SuspendUserRequest(
                UserSuspensionReason.SPAM, null, Duration.ofDays(1), true, false)).join())
                .hasCauseInstanceOf(BffProblems.InvalidSuspensionRequestProblem.class);
    }

    @Test
    void suspend_systemAccount_isRejected() {
        stubUser(new User(
                QuizUpConstants.SYSTEM_USER_ID, "system@quizup.dev", Set.of(), Instant.now()));

        assertThatThrownBy(() -> service.suspend(QuizUpConstants.SYSTEM_USER_ID, "admin-1",
                new SuspendUserRequest(UserSuspensionReason.OTHER, null, null, true, false)).join())
                .hasCauseInstanceOf(BffProblems.ProtectedAccountProblem.class);
    }

    @Test
    void suspend_alreadySuspended_isRejected() {
        Instant now = Instant.now();
        stubUser(new User(
                "user-2", "user2@quizup.dev", Set.of(), now, AccountStatus.SUSPENDED,
                new UserSuspension("s1", UserSuspensionReason.SPAM, null, "admin-0",
                        now, now.plusSeconds(3600), null, null, null, null)));

        assertThatThrownBy(() -> service.suspend("user-2", "admin-1",
                new SuspendUserRequest(UserSuspensionReason.HARASSMENT, null,
                        Duration.ofDays(1), false, false)).join())
                .hasCauseInstanceOf(BffProblems.UserAlreadySuspendedProblem.class);
    }

    private void stubUser(User user) {
        when(queryGateway.query(any(UserQuery.GetUserQuery.class),
                any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(user));
    }

    private static User activeUser() {
        return new User("user-2", "user2@quizup.dev", Set.of(), Instant.now());
    }
}
