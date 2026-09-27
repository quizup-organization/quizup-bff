package io.github.quizup.bff.infrastructure.in.api.response;

/**
 * Vue agrégée de la fiche sujet : carte (dont l'état de suivi) + rang réel + progression.
 */
public record TopicOverviewView(
        TopicCardView topic,
        Integer myRank,
        MyProgressView myProgress
) {

    public record MyProgressView(
            int xp,
            int level,
            String title,
            int xpForNextLevel,
            int levelProgressPercent
    ) {
    }
}
