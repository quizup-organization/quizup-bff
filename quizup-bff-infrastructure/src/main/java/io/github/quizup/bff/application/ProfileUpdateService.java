package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.problem.BffProblems;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.MasteryGrade;
import io.github.quizup.profile.domain.model.PlayerTitle;
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
    private final ProgressionLookup progressionLookup;

    public ProfileUpdateService(CommandGateway commandGateway,
                                ProfileLookup profileLookup,
                                ProgressionLookup progressionLookup) {
        this.commandGateway = commandGateway;
        this.profileLookup = profileLookup;
        this.progressionLookup = progressionLookup;
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

    public CompletableFuture<Void> updateBanner(String userId, String requestedBy, String bannerUrl) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfileBannerCommand(userId, requestedBy, bannerUrl));
    }

    public CompletableFuture<Void> updateLanguage(String userId, String requestedBy, Language language) {
        return dispatch(userId,
                new ProfileCommand.UpdateProfileLanguageCommand(userId, requestedBy, language));
    }

    /**
     * Titre thématique porté. La commande ne porte que la clé : la **légitimité** du titre
     * (sujet **joué** — XP > 0, l'entraînement bot n'ouvre rien — et grade réellement atteint
     * dans ce sujet) est vérifiée **ici**, avant l'envoi — l'agrégat ne connaît pas la progression
     * (`best-practices/.backend/command-preparation.md`).
     * {@code titleKey} {@code null} efface le titre porté.
     */
    public CompletableFuture<Void> updateEquippedTitle(String userId, String requestedBy, String titleKey) {
        if (titleKey == null) {
            return dispatch(userId,
                    new ProfileCommand.UpdateEquippedTitleCommand(userId, requestedBy, null));
        }

        PlayerTitle title = PlayerTitle.parse(titleKey)
                .orElseThrow(() -> new BffProblems.InvalidEquippedTitleProblem(titleKey));

        return progressionLookup.get(userId).thenCompose(progress -> {
            int topicXp = progress.xpForTopic(title.topicId());
            MasteryGrade reached = MasteryGrade.gradeFor(topicXp);
            if (topicXp <= 0 || reached.ordinal() < title.grade().ordinal()) {
                throw new BffProblems.TitleNotEarnedProblem(titleKey, reached.name());
            }

            return dispatch(userId,
                    new ProfileCommand.UpdateEquippedTitleCommand(userId, requestedBy, titleKey));
        });
    }

    private CompletableFuture<Void> dispatch(String userId, ProfileCommand command) {
        return commandGateway.send(command).thenRun(() -> profileLookup.invalidate(userId));
    }
}
