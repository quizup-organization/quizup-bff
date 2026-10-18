package io.github.quizup.bff.application;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.UserAccountStatus;
import io.github.quizup.identity.domain.query.UserQuery;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityLookupTest {

    @Mock
    private QueryGateway queryGateway;

    @Test
    void resolvesStatusFromIdentity() {
        UserAccountStatus status = new UserAccountStatus("user-1", AccountStatus.ACTIVE, null);
        when(queryGateway.query(any(UserQuery.GetUserAccountStatusQuery.class), any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(status));
        IdentityLookup lookup = new IdentityLookup(queryGateway);

        assertEquals(AccountStatus.ACTIVE, lookup.status("user-1").join().status());
    }

    @Test
    void cachesTheStatusAcrossCalls() {
        UserAccountStatus status = new UserAccountStatus(
                "user-1", AccountStatus.SUSPENDED, Instant.parse("2026-02-01T00:00:00Z"));
        when(queryGateway.query(any(UserQuery.GetUserAccountStatusQuery.class), any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(status));
        IdentityLookup lookup = new IdentityLookup(queryGateway);

        assertTrue(lookup.status("user-1").join().suspended(Instant.parse("2026-01-01T00:00:00Z")));
        assertTrue(lookup.status("user-1").join().suspended(Instant.parse("2026-01-01T00:00:00Z")));

        verify(queryGateway, times(1))
                .query(eq(new UserQuery.GetUserAccountStatusQuery("user-1")), any(ResponseType.class));
    }

    @Test
    void expiredSuspensionIsEffectivelyActive() {
        UserAccountStatus status = new UserAccountStatus(
                "user-1", AccountStatus.SUSPENDED, Instant.parse("2026-01-01T00:00:00Z"));
        assertFalse(status.suspended(Instant.parse("2026-02-01T00:00:00Z")));
    }
}
