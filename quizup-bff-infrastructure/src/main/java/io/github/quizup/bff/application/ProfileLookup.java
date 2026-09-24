package io.github.quizup.bff.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Résolution de profils avec cache local (Caffeine) et requête batch
 * ({@link ProfileQuery.GetProfilesByIdsQuery}) pour éviter le fan-out N+1 : le BFF est le point
 * d'agrégation, on amortit les lectures répétées et on résout les manquants en une requête.
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

    /** Résout plusieurs profils en une requête batch (cache + complément), dans l'ordre des ids demandés. */
    public CompletableFuture<List<Profile>> getAll(List<String> userIds) {
        Map<String, Profile> resolved = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (String userId : userIds) {
            if (resolved.containsKey(userId)) {
                continue;
            }
            Profile cached = cache.getIfPresent(userId);
            if (cached != null) {
                resolved.put(userId, cached);
            } else if (!missing.contains(userId)) {
                missing.add(userId);
            }
        }

        if (missing.isEmpty()) {
            return CompletableFuture.completedFuture(orderByIds(userIds, resolved));
        }

        return queryGateway
                .query(new ProfileQuery.GetProfilesByIdsQuery(missing), QueryResponseTypes.multipleInstancesOf(Profile.class))
                .thenApply(profiles -> {
                    profiles.forEach(profile -> {
                        cache.put(profile.userId(), profile);
                        resolved.put(profile.userId(), profile);
                    });
                    return orderByIds(userIds, resolved);
                });
    }

    private static List<Profile> orderByIds(List<String> userIds, Map<String, Profile> resolved) {
        return userIds.stream()
                .map(resolved::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
