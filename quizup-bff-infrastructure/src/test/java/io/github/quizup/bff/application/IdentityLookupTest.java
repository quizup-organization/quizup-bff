package io.github.quizup.bff.application;

import io.github.quizup.identity.domain.query.UserQuery;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

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
    void resolvesExistenceFromIdentity() {
        when(queryGateway.query(any(UserQuery.UserExistsByIdQuery.class), any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(true));
        IdentityLookup lookup = new IdentityLookup(queryGateway);

        assertTrue(lookup.exists("user-1").join());
    }

    @Test
    void cachesTheResultAcrossCalls() {
        when(queryGateway.query(any(UserQuery.UserExistsByIdQuery.class), any(ResponseType.class)))
                .thenReturn(CompletableFuture.completedFuture(false));
        IdentityLookup lookup = new IdentityLookup(queryGateway);

        assertFalse(lookup.exists("user-1").join());
        assertFalse(lookup.exists("user-1").join());

        verify(queryGateway, times(1)).query(eq(new UserQuery.UserExistsByIdQuery("user-1")), any(ResponseType.class));
    }
}
