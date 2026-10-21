package io.github.quizup.bff.infrastructure.in.api.response;

import java.time.Instant;

/**
 * Déblocage **célébré en direct** par le client (popup + jingle) : badge ou titre thématique.
 * Fan-out **éphémère** — comme {@link FollowPresenceView}, aucune persistance ni enveloppe : le
 * client affiche la célébration où qu'il soit (hors écran immersif) et la reporte à la sortie si
 * besoin.
 *
 * <p>Champs selon {@code kind} : {@code BADGE} → {@code code/label/family/tier/metric/target}
 * (l'objectif qui a permis l'acquisition) ; {@code TITLE} → {@code topicId/grade}. Les titres de
 * niveau de compte ne sont plus diffusés.</p>
 */
public record AchievementView(
        Kind kind,
        String code,
        String label,
        String family,
        String tier,
        String metric,
        Integer target,
        String topicId,
        String grade,
        Instant at
) {

    public enum Kind {
        BADGE,
        TITLE
    }

    /** Badge du catalogue gagné par le joueur (métrique et cible = objectif atteint). */
    public static AchievementView badge(String code, String label, String family, String tier,
                                        String metric, int target, Instant at) {
        return new AchievementView(Kind.BADGE, code, label, family, tier, metric, target,
                null, null, at);
    }

    /** Titre thématique débloqué : grade de maîtrise franchi dans le sujet. */
    public static AchievementView title(String topicId, String grade, Instant at) {
        return new AchievementView(Kind.TITLE, null, null, null, null, null, null,
                topicId, grade, at);
    }
}
