package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.TopicStatView;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Composition du donut « Sujets joués » : progression par thème enrichie des informations
 * éditoriales du sujet, sujets introuvables ignorés.
 */
class ProfileViewServiceTest {

    private final QueryGateway queryGateway = mock(QueryGateway.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final ProgressionLookup progressionLookup = mock(ProgressionLookup.class);
    private final FollowLookup followLookup = mock(FollowLookup.class);
    private final ProfileViewService service =
            new ProfileViewService(queryGateway, profileLookup, progressionLookup, followLookup);

    @Test
    void topics_enrich_progressions_and_skip_missing_topics() {
        when(queryGateway.query(
                any(ProgressionQuery.GetTopicProgressionsQuery.class),
                ArgumentMatchers.<ResponseType<List<TopicProgress>>>any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new TopicProgress("topic-1", 200, 2, 7),
                        new TopicProgress("missing", 50, 1, 2))));
        when(queryGateway.query(
                any(TopicQuery.GetTopicsByIdsQuery.class),
                ArgumentMatchers.<ResponseType<List<Topic>>>any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        Topic.builder()
                                .topicId("topic-1")
                                .names(Map.of(Language.FR, "DotA"))
                                .emoji("🎮")
                                .color("#f00")
                                .imageUrl("https://example.com/img.png")
                                .build())));

        List<TopicStatView> view = service.topics("me").join();

        assertThat(view).hasSize(1);
        assertThat(view.get(0).topicId()).isEqualTo("topic-1");
        assertThat(view.get(0).names()).containsEntry(Language.FR, "DotA");
        assertThat(view.get(0).emoji()).isEqualTo("🎮");
        assertThat(view.get(0).games()).isEqualTo(7);
        assertThat(view.get(0).xp()).isEqualTo(200);
        assertThat(view.get(0).level()).isEqualTo(2);
    }

    @Test
    void topics_skip_topic_lookup_when_no_progressions() {
        when(queryGateway.query(
                any(ProgressionQuery.GetTopicProgressionsQuery.class),
                ArgumentMatchers.<ResponseType<List<TopicProgress>>>any()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        List<TopicStatView> view = service.topics("me").join();

        assertThat(view).isEmpty();
    }
}
