package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.SuggestionView;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.profile.domain.query.ProfileQuery;
import io.github.quizup.theme.domain.model.TopicPage;
import io.github.quizup.theme.domain.model.TopicSort;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Suggestions de la palette ⌘K : sujets puis joueurs.
 */
@Service
public class SuggestionService {

    private final QueryGateway queryGateway;

    public SuggestionService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    public CompletableFuture<List<SuggestionView>> suggest(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return CompletableFuture.completedFuture(List.of());
        }

        CompletableFuture<TopicPage> topicsFuture = queryGateway.query(
                new TopicQuery.GetTopicPageQuery(query, null, TopicSort.POPULAR, 0, limit),
                QueryResponseTypes.instanceOf(TopicPage.class));
        CompletableFuture<List<Profile>> playersFuture = queryGateway.query(
                new ProfileQuery.GetProfileSuggestionsQuery(query, limit),
                QueryResponseTypes.multipleInstancesOf(Profile.class));

        return CompletableFuture.allOf(topicsFuture, playersFuture).thenCompose(_ -> {
            List<Profile> players = playersFuture.join();
            List<String> playerIds = players.stream().map(Profile::userId).toList();
            CompletableFuture<List<PlayerProgress>> progressionsFuture = playerIds.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : queryGateway.query(new ProgressionQuery.GetProgressionsByIdsQuery(playerIds),
                            QueryResponseTypes.multipleInstancesOf(PlayerProgress.class));

            return progressionsFuture.thenApply(progressions -> {
                Map<String, PlayerProgress> progressById = progressions.stream()
                        .collect(Collectors.toMap(PlayerProgress::userId, Function.identity(), (first, _) -> first));
                List<SuggestionView> suggestions = new ArrayList<>();
                topicsFuture.join().topics().forEach(topic -> suggestions.add(new SuggestionView(
                        SuggestionView.Type.TOPIC,
                        topic.topicId(),
                        topic.name(),
                        topic.category() == null ? null : topic.category().label(),
                        topic.emoji(),
                        topic.color(),
                        topic.imageUrl(),
                        null)));
                players.forEach(profile -> suggestions.add(new SuggestionView(
                        SuggestionView.Type.PLAYER,
                        profile.userId(),
                        profile.pseudonym(),
                        "Niveau " + ProgressionViews.toView(
                                progressById.getOrDefault(profile.userId(),
                                        PlayerProgress.empty(profile.userId()))).level(),
                        null,
                        null,
                        null,
                        profile.avatarOptions())));
                return List.copyOf(suggestions);
            });
        });
    }
}
