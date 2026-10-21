package io.github.quizup.bff.infrastructure.config;

import io.github.quizup.bff.application.DuelLimitsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Active les limites de création de duels ({@code quizup.duel.limits.*}). */
@Configuration
@EnableConfigurationProperties(DuelLimitsProperties.class)
public class DuelLimitsConfiguration {
}
