package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.AccountDeletionService;
import io.github.quizup.bff.application.MeService;
import io.github.quizup.bff.application.ProfileUpdateService;
import io.github.quizup.bff.infrastructure.in.api.request.DeleteAccountRequest;
import io.github.quizup.bff.infrastructure.in.api.response.MeView;
import io.github.quizup.microservice.security.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/me} — joueur courant (profil, progression, compteurs, badge de défis).
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;
    private final AccountDeletionService accountDeletionService;
    private final ProfileUpdateService profileUpdateService;

    public MeController(MeService meService,
                        AccountDeletionService accountDeletionService,
                        ProfileUpdateService profileUpdateService) {
        this.meService = meService;
        this.accountDeletionService = accountDeletionService;
        this.profileUpdateService = profileUpdateService;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<MeView>> me() {
        return meService.me(SecurityHelper.getUserId()).thenApply(ResponseEntity::ok);
    }

    /** Termine (ou passe) l'onboarding du joueur courant. */
    @PostMapping("/onboarding/complete")
    public CompletableFuture<ResponseEntity<MeView>> completeOnboarding() {
        String userId = SecurityHelper.getUserId();
        return profileUpdateService.completeOnboarding(userId)
                .thenCompose(_ -> meService.me(userId))
                // La projection profile est asynchrone : le flag renvoyé doit refléter la commande
                // déjà acceptée, sinon le guard web renvoie le joueur sur l'onboarding.
                .thenApply(view -> ResponseEntity.ok(view.withOnboardingCompleted(true)));
    }

    /** Envoie le code OTP de confirmation de suppression à l'email du compte. */
    @PostMapping("/deletion-code")
    public ResponseEntity<Void> requestDeletionCode() {
        accountDeletionService.requestCode(SecurityHelper.getUserId());
        return ResponseEntity.accepted().build();
    }

    /** Supprime définitivement le compte après vérification du code OTP. */
    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
        accountDeletionService.delete(SecurityHelper.getUserId(), request.code());
        return ResponseEntity.accepted().build();
    }
}
