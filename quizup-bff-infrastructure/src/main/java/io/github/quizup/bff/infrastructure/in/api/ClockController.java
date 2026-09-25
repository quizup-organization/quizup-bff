package io.github.quizup.bff.infrastructure.in.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Horloge serveur — ressource système {@code GET /api/clock}.
 *
 * <p>Utilisée par le client pour synchroniser son chrono de duel sur l'heure serveur.
 * Contrat aligné sur l'ancien {@code GET /api/games/time} ({@code serverTime}, {@code epochMillis}).</p>
 */
@RestController
@RequestMapping("/api")
public class ClockController {

    @GetMapping("/clock")
    public ResponseEntity<Map<String, Object>> clock() {
        Instant now = Instant.now();
        return ResponseEntity.ok(Map.of(
                "serverTime", now,
                "epochMillis", now.toEpochMilli()
        ));
    }
}
