package io.github.quizup.bff.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Active la sécurité par méthode pour la surface d'administration média
 * ({@code @PreAuthorize("hasRole('ADMIN')")} dans {@code MediaAdminController}). Le décodage JWT,
 * la chaîne de filtres et le mapping du claim {@code roles} vers {@code ROLE_*} proviennent du
 * SDK (resource server) : aucune règle d'URL supplémentaire ici.
 */
@Configuration
@EnableMethodSecurity
public class AdminSecurityConfiguration {
}
