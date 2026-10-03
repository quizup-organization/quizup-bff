package io.github.quizup.bff.application;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;

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
    private final ProfileUpdateService service = new ProfileUpdateService(commandGateway, profileLookup);

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
    void failedCommandDoesNotInvalidateCache() {
        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("boom"));
        when(commandGateway.send(any())).thenReturn(failed);

        assertThatThrownBy(() -> service.updateBio("u1", "u1", "bio").join())
                .isInstanceOf(CompletionException.class);
        verifyNoInteractions(profileLookup);
    }
}
