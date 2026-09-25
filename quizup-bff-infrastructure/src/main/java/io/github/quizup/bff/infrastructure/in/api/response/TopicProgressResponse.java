package io.github.quizup.bff.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * DTO de progression d'un joueur dans un thème (XP, niveau dérivé + titre).
 */
public record TopicProgressResponse(
        String topicId,
        int xp,
        int level,
        String title
) implements Serializable {
}
