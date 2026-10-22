package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.AccountDeletionService;
import io.github.quizup.bff.application.MeService;
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

    public MeController(MeService meService,
                        AccountDeletionService accountDeletionService) {
        this.meService = meService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<MeView>> me() {
        return meService.me(SecurityHelper.getUserId()).thenApply(ResponseEntity::ok);
    }

    /** Envoie le code OTP de confirmation de suppression à l'email du compte. */
    @PostMapping("/deletion-code")
    public ResponseEntity<Void> requestDeletionCode() {
        accountDeletionService.requestCode(SecurityHelper.getUserId());
        return ResponseEntity.accepted().build();
    }

    /** Supprime définitivement le compte après vérification du code OTP et de la phrase. */
    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
        accountDeletionService.delete(
                SecurityHelper.getUserId(), request.code(), request.confirmation());
        return ResponseEntity.accepted().build();
    }
}
