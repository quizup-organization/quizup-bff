package io.github.quizup.bff.infrastructure.in.api.mapper;

import io.github.quizup.bff.infrastructure.in.api.response.BadgeResponse;
import io.github.quizup.bff.infrastructure.in.api.response.DuelStatsResponse;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionResponse;
import io.github.quizup.bff.infrastructure.in.api.response.TopicProgressResponse;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.model.TopicProgress;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Adapte le read model de progression au contrat REST exposé par le BFF
 * (identique à l'ancien DTO du service profile : {@code xpForNextLevel}, {@code duelStats},
 * badges et progression par thème).
 */
public final class ProgressionResponseMapper {

    private ProgressionResponseMapper() {
    }

    public static ProgressionResponse toResponse(PlayerProgress progress) {
        List<BadgeResponse> badges = progress.badges().stream()
                .sorted(Comparator.comparing(Badge::name))
                .map(badge -> new BadgeResponse(badge.name(), badge.label()))
                .toList();

        List<TopicProgressResponse> topics = progress.xpByTopic().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed())
                .map(entry -> {
                    int level = ProgressionRules.levelFor(entry.getValue());
                    return new TopicProgressResponse(
                            entry.getKey(),
                            entry.getValue(),
                            level,
                            ProgressionRules.titleFor(level));
                })
                .toList();

        int winRate = progress.gamesPlayed() > 0
                ? Math.round(progress.wins() * 100f / progress.gamesPlayed())
                : 0;

        DuelStatsResponse duelStats = new DuelStatsResponse(
                progress.gamesPlayed(),
                progress.wins(),
                progress.losses(),
                winRate,
                progress.bestScore(),
                progress.bestWinStreak()
        );

        return new ProgressionResponse(
                progress.userId(),
                progress.xpTotal(),
                progress.level(),
                progress.title(),
                progress.xpForNextLevel(),
                badges,
                topics,
                duelStats
        );
    }

    public static TopicProgressResponse toResponse(TopicProgress topicProgress) {
        int level = ProgressionRules.levelFor(topicProgress.xp());
        return new TopicProgressResponse(
                topicProgress.topicId(),
                topicProgress.xp(),
                level,
                ProgressionRules.titleFor(level)
        );
    }
}
