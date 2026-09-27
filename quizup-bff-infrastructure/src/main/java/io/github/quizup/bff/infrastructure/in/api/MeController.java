package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.MeService;
import io.github.quizup.bff.infrastructure.in.api.response.MeView;
import io.github.quizup.microservice.security.SecurityHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<MeView>> me() {
        return meService.me(SecurityHelper.getUserId()).thenApply(ResponseEntity::ok);
    }
}
