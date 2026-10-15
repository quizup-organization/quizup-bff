package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.BadgeCollectionView;
import io.github.quizup.bff.infrastructure.in.api.response.TitlesView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicStatView;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
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
import java.util.Set;
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

    @Test
    void titles_expose_reached_grades_sorted_and_carry_the_equipped_key() {
        when(profileLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(
                Profile.builder().userId("user-1").equippedTitle("topic-2:EXPERT").build()));
        when(progressionLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(
                PlayerProgress.empty("user-1").toBuilder()
                        .xpByTopic(Map.of(
                                "topic-1", 2_600,   // Confirmé
                                "topic-2", 6_000,   // Expert
                                "topic-3", 120))    // Novice → aucun titre
                        .build()));
        when(queryGateway.query(
                any(TopicQuery.GetTopicsByIdsQuery.class),
                ArgumentMatchers.<ResponseType<List<Topic>>>any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        Topic.builder().topicId("topic-1").names(Map.of(Language.FR, "Histoire")).build(),
                        Topic.builder().topicId("topic-2").names(Map.of(Language.FR, "Java")).build())));

        TitlesView view = service.titles("user-1").join();

        assertThat(view.equippedKey()).isEqualTo("topic-2:EXPERT");
        assertThat(view.titles()).extracting(TitlesView.TitleView::key)
                .containsExactly("topic-2:EXPERT", "topic-1:CONFIRME");
        assertThat(view.titles()).extracting(TitlesView.TitleView::grade)
                .containsExactly("EXPERT", "CONFIRME");
        assertThat(view.titles().get(0).topic().names()).containsEntry(Language.FR, "Java");
    }

    @Test
    void titles_skip_the_topic_lookup_when_no_grade_is_reached() {
        when(profileLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(
                Profile.builder().userId("user-1").build()));
        when(progressionLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(
                PlayerProgress.empty("user-1").toBuilder()
                        .xpByTopic(Map.of("topic-1", 120))
                        .build()));

        TitlesView view = service.titles("user-1").join();

        assertThat(view.equippedKey()).isNull();
        assertThat(view.titles()).isEmpty();
    }

    @Test
    void badges_expose_the_whole_catalogue_with_capped_progress() {
        when(progressionLookup.get("user-1")).thenReturn(CompletableFuture.completedFuture(
                PlayerProgress.empty("user-1").toBuilder()
                        .gamesPlayed(12)
                        .wins(3)
                        .perfectDuels(4)
                        .fastAnswersTotal(80)
                        .bestDayStreak(5)
                        .xpByTopic(Map.of("topic-1", 2_600))
                        .badges(Set.of(Badge.FIRST_WIN, Badge.DUELIST_BRONZE))
                        .build()));

        BadgeCollectionView view = service.badges("user-1").join();

        assertThat(view.earnedCount()).isEqualTo(2);
        assertThat(view.totalCount()).isEqualTo(Badge.values().length);
        assertThat(view.families()).extracting(BadgeCollectionView.FamilyView::family)
                .containsExactly("VOLUME", "MASTERY", "EXPLOIT", "SERIES", "ASSIDUITY",
                        "PRECISION", "SPEED", "EXPLORATION", "COLLECTION");

        // Volume : acquis au palier bronze (plafonné), en cours pour l'argent.
        assertThat(item(view, Badge.DUELIST_BRONZE).earned()).isTrue();
        assertThat(item(view, Badge.DUELIST_BRONZE).progress()).isEqualTo(10);
        assertThat(item(view, Badge.DUELIST_ARGENT).earned()).isFalse();
        assertThat(item(view, Badge.DUELIST_ARGENT).progress()).isEqualTo(12);
        assertThat(item(view, Badge.WINNER_BRONZE).progress()).isEqualTo(3);

        // Maîtrise : meilleur sujet du joueur (2 600 XP), plafonné au seuil de Confirmé.
        assertThat(item(view, Badge.MASTER_BRONZE).progress()).isEqualTo(2_500);

        // Collection : nombre de badges acquis ; exploits : faits ponctuels.
        assertThat(item(view, Badge.COLLECTOR_BRONZE).progress()).isEqualTo(2);
        assertThat(item(view, Badge.FIRST_WIN).progress()).isEqualTo(1);
        assertThat(item(view, Badge.PERFECT).progress()).isZero();

        // Précision / Rapidité : compteurs d'exploits cumulés, plafonnés au seuil du palier.
        assertThat(item(view, Badge.PRECISION_BRONZE).progress()).isEqualTo(4);
        assertThat(item(view, Badge.SPEED_BRONZE).progress()).isEqualTo(50);

        // Assiduité : meilleure série de jours joués, plafonnée au seuil du palier (3).
        assertThat(item(view, Badge.ASSIDUITY_BRONZE).progress()).isEqualTo(3);
    }

    private static BadgeCollectionView.BadgeItemView item(BadgeCollectionView view, Badge badge) {
        return view.families().stream()
                .flatMap(family -> family.badges().stream())
                .filter(candidate -> candidate.code().equals(badge.name()))
                .findFirst()
                .orElseThrow();
    }
}
