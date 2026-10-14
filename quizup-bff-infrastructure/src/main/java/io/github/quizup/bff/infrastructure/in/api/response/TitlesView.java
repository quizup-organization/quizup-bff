package io.github.quizup.bff.infrastructure.in.api.response;

import java.util.List;

/**
 * Titres thématiques d'un joueur : le **titre porté** ({@code equippedKey}) et les titres qu'il a
 * débloqués — un par sujet où il a atteint au moins le premier grade. Le libellé n'est pas
 * composé côté serveur : le client combine le grade ({@code grade}, à traduire) et le nom du
 * sujet ({@code topic.names}, dans la langue du lecteur).
 */
public record TitlesView(
        String equippedKey,
        List<TitleView> titles
) {

    public record TitleView(
            /** Clé stable du titre, telle qu'attendue par `PUT /api/profiles/{id}/title`. */
            String key,
            String topicId,
            /** Grade atteint dans ce sujet (`MasteryGrade`), ex. {@code CONFIRME}. */
            String grade,
            /** Progression (0–100) vers le grade suivant du sujet. */
            int gradeProgressPercent,
            /** XP restante dans ce sujet avant le grade suivant (0 au dernier grade). */
            int gradeRemainingXp,
            /** Grade suivant du sujet ({@code null} au dernier grade). */
            String nextGrade,
            TopicRefView topic
    ) {
    }
}
