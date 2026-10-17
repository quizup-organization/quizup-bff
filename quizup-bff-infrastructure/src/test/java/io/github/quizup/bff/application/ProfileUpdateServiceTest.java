package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.PlayerProgress;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Une écriture de profil réussie doit purger le cache de lecture ({@link ProfileLookup}),
 * sinon les vues servent l'ancien profil jusqu'à l'expiration du TTL (sauvegarde perçue
 * comme perdue).
 */
class ProfileUpdateServiceTest {

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final ProfileLookup profileLookup = mock(ProfileLookup.class);
    private final ProgressionLookup progressionLookup = mock(ProgressionLookup.class);
    private final ProfileUpdateService service =
            new ProfileUpdateService(commandGateway, profileLookup, progressionLookup);

    @Test
    void updatePseudonymInvalidatesCacheOnSuccess() {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updatePseudonym("u1", "u1", "Nouveau").join();

        verify(profileLookup).invalidate("u1");
    }

    @Test
    void updateLanguageInvalidatesCacheOnSuccess() {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updateLanguage("u1", "u1", Language.EN).join();

        verify(profileLookup).invalidate("u1");
    }

    @Test
    void updateBannerInvalidatesCacheOnSuccess() {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updateBanner("u1", "u1", "https://media.example.test/banner.jpg").join();

        verify(profileLookup).invalidate("u1");
    }

    @Test
    void failedCommandDoesNotInvalidateCache() {
        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("boom"));
        when(commandGateway.send(any())).thenReturn(failed);

        assertThatThrownBy(() -> service.updateBio("u1", "u1", "bio").join())
                .isInstanceOf(CompletionException.class);
        verifyNoInteractions(profileLookup);
    }

    @Test
    void equippedTitle_sendsTheCommandAndInvalidatesCacheWhenEarned() {
        stubProgress(Map.of("topic-1", 2_600));
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updateEquippedTitle("u1", "u1", "topic-1:CONFIRME").join();

        verify(commandGateway).send(new ProfileCommand.UpdateEquippedTitleCommand(
                "u1", "u1", "topic-1:CONFIRME"));
        verify(profileLookup).invalidate("u1");
    }

    @Test
    void equippedTitle_isRejectedAndNoCommandIsSentWhenTheGradeIsNotReached() {
        stubProgress(Map.of("topic-1", 2_600));

        assertThatThrownBy(() -> service.updateEquippedTitle("u1", "u1", "topic-1:EXPERT").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BffProblems.TitleNotEarnedProblem.class);
        verifyNoInteractions(commandGateway);
        verifyNoInteractions(profileLookup);
    }

    @Test
    void equippedTitle_noviceIsAllowedOnceTheTopicHasBeenPlayed() {
        stubProgress(Map.of("topic-1", 120));
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updateEquippedTitle("u1", "u1", "topic-1:NOVICE").join();

        verify(commandGateway).send(new ProfileCommand.UpdateEquippedTitleCommand(
                "u1", "u1", "topic-1:NOVICE"));
        verify(profileLookup).invalidate("u1");
    }

    @Test
    void equippedTitle_noviceOnUnplayedTopic_isRejectedAsNotEarned() {
        stubProgress(Map.of("topic-1", 0), Map.of());

        assertThatThrownBy(() -> service.updateEquippedTitle("u1", "u1", "topic-1:NOVICE").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BffProblems.TitleNotEarnedProblem.class);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void equippedTitle_noviceIsAllowedAfterAHumanGameEvenAtZeroXp() {
        stubProgress(Map.of("topic-1", 0), Map.of("topic-1", 1));
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updateEquippedTitle("u1", "u1", "topic-1:NOVICE").join();

        verify(commandGateway).send(new ProfileCommand.UpdateEquippedTitleCommand(
                "u1", "u1", "topic-1:NOVICE"));
        verify(profileLookup).invalidate("u1");
    }

    @Test
    void equippedTitle_noviceOnXpOnlyTopicWithoutHumanGame_isRejected() {
        stubProgress(Map.of("topic-1", 120), Map.of());

        assertThatThrownBy(() -> service.updateEquippedTitle("u1", "u1", "topic-1:NOVICE").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BffProblems.TitleNotEarnedProblem.class);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void equippedTitle_withUnknownTopic_isRejectedAsNotEarned() {
        stubProgress(Map.of("topic-1", 30_000));

        assertThatThrownBy(() -> service.updateEquippedTitle("u1", "u1", "topic-9:LEGENDE").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BffProblems.TitleNotEarnedProblem.class);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void equippedTitle_withMalformedKey_isRejectedWithoutReadingProgression() {
        assertThatThrownBy(() -> service.updateEquippedTitle("u1", "u1", "topic-1").join())
                .isInstanceOf(BffProblems.InvalidEquippedTitleProblem.class);
        verifyNoInteractions(progressionLookup);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void equippedTitle_withNull_clearsItWithoutReadingProgression() {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        service.updateEquippedTitle("u1", "u1", null).join();

        verify(commandGateway).send(new ProfileCommand.UpdateEquippedTitleCommand("u1", "u1", null));
        verifyNoInteractions(progressionLookup);
    }

    private void stubProgress(Map<String, Integer> xpByTopic) {
        Map<String, Integer> gamesByTopic = new java.util.HashMap<>();
        xpByTopic.keySet().forEach(topicId -> gamesByTopic.put(topicId, 1));
        stubProgress(xpByTopic, gamesByTopic);
    }

    private void stubProgress(Map<String, Integer> xpByTopic,
                              Map<String, Integer> gamesByTopic) {
        when(progressionLookup.get("u1")).thenReturn(CompletableFuture.completedFuture(
                PlayerProgress.empty("u1").toBuilder()
                        .xpByTopic(xpByTopic)
                        .gamesByTopic(gamesByTopic)
                        .build()));
    }
}
