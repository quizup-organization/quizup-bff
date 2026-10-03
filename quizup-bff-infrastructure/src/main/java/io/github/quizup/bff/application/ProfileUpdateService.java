package io.github.quizup.bff.application;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.command.ProfileCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Écritures du profil : envoie la commande Axon puis **purge le cache de lecture**
 * ({@link ProfileLookup}) sur succès. Sans cette invalidation, {@code GET /api/me} et les vues
 * enrichies servent l'ancien profil jusqu'à l'expiration du TTL (30 s), donnant l'impression
 * d'une sauvegarde perdue. L'invalidation s'applique aussi aux commandes no-op (valeur
 * inchangée) : la prochaine lecture re-projette la vérité.
 */
@Service
public class ProfileUpdateService {

    private final CommandGateway commandGateway;
    private final ProfileLookup profileLookup;

    public ProfileUpdateService(CommandGateway commandGateway, ProfileLookup profileLookup) {
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
    }

    public CompletableFuture<Void> updatePseudonym(String userId, String requestedBy, String pseudonym) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfilePseudonymCommand(userId, requestedBy, pseudonym));
    }

    public CompletableFuture<Void> updateBio(String userId, String requestedBy, String bio) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfileBioCommand(userId, requestedBy, bio));
    }

    public CompletableFuture<Void> updateCountry(String userId, String requestedBy, String country) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfileCountryCommand(userId, requestedBy, country));
    }

    public CompletableFuture<Void> updateAvatarOptions(String userId, String requestedBy, String avatarOptions) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfileAvatarCommand(userId, requestedBy, avatarOptions));
    }

    public CompletableFuture<Void> updateLanguage(String userId, String requestedBy, Language language) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfileLanguageCommand(userId, requestedBy, language));
    }

    private CompletableFuture<Void> dispatch(String userId, ProfileCommand command) {
        return commandGateway.send(command).thenRun(() -> profileLookup.invalidate(userId));
    }
}
