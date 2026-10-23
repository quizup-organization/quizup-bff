package io.github.quizup.bff.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;

/**
 * Vue agrégée de la fiche sujet : carte (dont l'état de suivi) + rang réel + progression.
 * {@code canManage} indique au client que le joueur courant est le créateur du sujet.
 */
public record TopicOverviewView(
        TopicCardView topic,
        /**
         * Titres **thématiques** du sujet par langue puis grade de maîtrise (ex.
         * `{ fr: { CONFIRME: "Historien" } }`) : alimente l'onglet « Titres » de la fiche sujet.
         * Le libellé affiché est composé côté client (grade traduit + nom du sujet).
         */
        Map<Language, Map<String, String>> titles,
        Integer myRank,
        MyProgressView myProgress,
        boolean canManage
) {

    public record MyProgressView(
            int xp,
            /** Parties humaines terminées dans le sujet (abandon inclus) — condition du titre Fer. */
            int games,
            /** Grade de **maîtrise du sujet** atteint (`MasteryGrade`), ex. {@code CONFIRME}. */
            String grade,
            /** Progression (0–100) vers le grade suivant du sujet ; 100 au dernier grade. */
            int gradeProgressPercent,
            /** XP restante dans ce sujet avant le grade suivant (0 au dernier grade). */
            int gradeRemainingXp,
            /** Grade suivant du sujet ({@code null} au dernier grade). */
            String nextGrade,
            /** Questions distinctes du sujet effectivement répondues par le joueur. */
            int completedQuestions,
            /** Nombre total de questions approuvées du sujet. */
            int totalQuestions,
            /** Complétion du sujet en pourcentage (`completedQuestions / totalQuestions`). */
            int completionPercent
    ) {
    }
}
