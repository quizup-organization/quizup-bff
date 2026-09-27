package io.github.quizup.bff.infrastructure.in.api.response;

import java.util.List;

/**
 * Données de l'accueil : sujets suivis (récents) et sujets les plus joués.
 */
public record HomeView(
        List<TopicCardView> followedTopics,
        List<TopicCardView> trendingTopics
) {
}
