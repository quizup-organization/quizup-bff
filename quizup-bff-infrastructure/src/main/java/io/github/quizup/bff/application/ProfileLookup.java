package io.github.quizup.bff.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Résolution de profils avec cache local (Caffeine) : le BFF étant le point d'agrégation,
 * on amortit les lectures répétées (listes de suiveurs, portées de classement, noms d'adversaire).
 */
@Service
public class ProfileLookup {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final long MAX_SIZE = 10_000;

    private final QueryGateway queryGateway;
    private final Cache<String, Profile> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(MAX_SIZE)
            .build();

    public ProfileLookup(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    public CompletableFuture<Profile> get(String userId) {
        Profile cached = cache.getIfPresent(userId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return queryGateway
                .query(new ProfileQuery.GetProfileQuery(userId), QueryResponseTypes.instanceOf(Profile.class))
                .thenApply(profile -> {
                    cache.put(userId, profile);
                    return profile;
                });
    }

    /** Résout plusieurs profils en parallèle (avec cache), dans l'ordre des ids demandés. */
    public CompletableFuture<List<Profile>> getAll(List<String> userIds) {
        List<CompletableFuture<Profile>> futures = userIds.stream().map(this::get).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> futures.stream().map(CompletableFuture::join).toList());
    }
}
