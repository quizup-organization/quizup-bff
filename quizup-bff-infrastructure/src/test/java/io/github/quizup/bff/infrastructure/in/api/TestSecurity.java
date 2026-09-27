package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Authentification de test : pose un {@link QuizUpPrincipal} dans le contexte de sécurité,
 * ce que fait normalement le JWT resource server.
 */
final class TestSecurity {

    private TestSecurity() {
    }

    static void authenticate(String userId) {
        QuizUpPrincipal principal = new QuizUpPrincipal() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getEmail() {
                return userId + "@quizup.io";
            }
        };
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    static void clear() {
        SecurityContextHolder.clearContext();
    }
}
