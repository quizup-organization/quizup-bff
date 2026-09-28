package io.github.quizup.bff.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.quizup.identity.domain.query.UserQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Existence d'un utilisateur côté identity, avec cache local (Caffeine) : le JWT reste
 * cryptographiquement valide après une purge de base, ce contrôle détecte les sessions
 * périmées sans interroger identity à chaque requête.
 */
@Service
public class IdentityLookup {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final long MAX_SIZE = 10_000;

    private final QueryGateway queryGateway;
    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(MAX_SIZE)
            .build();

    public IdentityLookup(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    public CompletableFuture<Boolean> exists(String userId) {
        Boolean cached = cache.getIfPresent(userId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return queryGateway
                .query(new UserQuery.UserExistsByIdQuery(userId), QueryResponseTypes.instanceOf(Boolean.class))
                .thenApply(exists -> {
                    cache.put(userId, exists);
                    return exists;
                });
    }
}
