package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Vue agrégée de la fiche sujet : carte (dont l'état de suivi) + rang réel + progression.
 * {@code canManage} indique au client que le joueur courant est le créateur du sujet.
 */
public record TopicOverviewView(
        TopicCardView topic,
        Integer myRank,
        MyProgressView myProgress,
        boolean canManage
) {

    public record MyProgressView(
            int xp,
            int level,
            String title,
            int xpForNextLevel,
            int levelProgressPercent,
            /** Questions distinctes du sujet effectivement répondues par le joueur. */
            int completedQuestions,
            /** Nombre total de questions approuvées du sujet. */
            int totalQuestions,
            /** Complétion du sujet en pourcentage (`completedQuestions / totalQuestions`). */
            int completionPercent
    ) {
    }
}
