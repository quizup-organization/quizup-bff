package io.github.quizup.bff.infrastructure.out.media;

import io.github.quizup.bff.application.MediaReferenceChecker;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.query.ProfileQuery;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Vérification des références média via le query bus distribué : comptage theme (couverture,
 * bannière de sujet, illustration de question) + profile (bannière de profil), fusionné par URL.
 * Toute indisponibilité remonte à l'appelant, qui échoue fermé pour les suppressions.
 */
@Component
public class MediaReferenceCheckerAdapter implements MediaReferenceChecker {

    private final QueryGateway queryGateway;

    public MediaReferenceCheckerAdapter(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Map<String, Long>> countReferences(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        CompletableFuture<List<io.github.quizup.theme.domain.model.ImageReferenceCount>> themeCounts =
                queryGateway.query(new TopicQuery.CountImageReferencesQuery(urls),
                        QueryResponseTypes.multipleInstancesOf(
                                io.github.quizup.theme.domain.model.ImageReferenceCount.class));
        CompletableFuture<List<io.github.quizup.profile.domain.model.ImageReferenceCount>> profileCounts =
                queryGateway.query(new ProfileQuery.CountImageReferencesQuery(urls),
                        QueryResponseTypes.multipleInstancesOf(
                                io.github.quizup.profile.domain.model.ImageReferenceCount.class));
        return themeCounts.thenCombine(profileCounts, (theme, profiles) -> merge(urls, theme, profiles));
    }

    private static Map<String, Long> merge(List<String> urls,
                                           List<io.github.quizup.theme.domain.model.ImageReferenceCount> themeCounts,
                                           List<io.github.quizup.profile.domain.model.ImageReferenceCount> profileCounts) {
        Map<String, Long> counts = new HashMap<>();
        urls.forEach(url -> counts.put(url, 0L));
        themeCounts.forEach(count -> counts.merge(count.imageUrl(), count.count(), Long::sum));
        profileCounts.forEach(count -> counts.merge(count.imageUrl(), count.count(), Long::sum));
        return counts;
    }
}
