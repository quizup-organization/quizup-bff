package io.github.quizup.bff.infrastructure.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.application.IdentityLookup;
import io.github.quizup.microservice.MicroserviceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Rejette en {@code 401} les JWT encore valides dont l'utilisateur n'existe plus côté identity
 * (base purgée/reset, compte supprimé). Sans ce garde-fou, ces requêtes produisent des
 * {@code profile:notFound} en cascade et des lobbies incohérents (saga matchmaking).
 * Le client purge alors sa session et repasse par le login.
 * <p>
 * Fail-open : si identity est injoignable, le jeton reste la preuve d'authentification et la
 * requête passe (la disponibilité prime sur le nettoyage des sessions périmées).
 * <p>
 * La réponse 401 porte les en-têtes CORS de la configuration du service : le filtre
 * court-circuite **avant** le {@code CorsFilter} servlet (même ordre par défaut), et sans eux
 * le navigateur masque la réponse — le web ne verrait jamais le 401 et ne purgerait pas la
 * session (écran figé jusqu'à l'expiration de l'access token).
 */
@Component
public class CurrentUserExistsFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CurrentUserExistsFilter.class);

    private static final long LOOKUP_TIMEOUT_MS = 3_000;
    private static final String USER_ID_CLAIM = "user_id";

    private final IdentityLookup identityLookup;
    private final ObjectMapper objectMapper;
    private final CorsConfiguration corsConfiguration;

    public CurrentUserExistsFilter(IdentityLookup identityLookup,
                                   ObjectMapper objectMapper,
                                   MicroserviceProperties properties) {
        this.identityLookup = identityLookup;
        this.objectMapper = objectMapper;
        this.corsConfiguration = corsConfiguration(properties);
    }

    /** Mêmes origines/credentials que {@code CorsAutoConfiguration} du SDK (source des headers). */
    private static CorsConfiguration corsConfiguration(MicroserviceProperties properties) {
        MicroserviceProperties.Cors cors = properties.cors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(cors.allowCredentials());
        if (cors.allowCredentials() && cors.allowedOrigins().contains("*")) {
            configuration.addAllowedOriginPattern("*");
        } else {
            cors.allowedOrigins().forEach(configuration::addAllowedOrigin);
        }
        return configuration;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtAuthentication.getToken().getClaimAsString(USER_ID_CLAIM);
        if (userId == null || userId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (Boolean.FALSE.equals(exists(userId))) {
            logger.warn("Stale session rejected: user {} no longer exists in identity", userId);
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** {@code null} = indéterminé (identity injoignable ou trop lent) : fail-open. */
    private Boolean exists(String userId) {
        try {
            return identityLookup.exists(userId).get(LOOKUP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Identity lookup interrupted for {} (fail-open)", userId);
        } catch (ExecutionException | TimeoutException e) {
            logger.warn("Identity lookup failed for {} (fail-open): {}", userId, e.toString());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", error_description=\"unknown user\"");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "urn:quizup:authentification:unknownUser");
        body.put("title", "Session invalide");
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("detail", "L'utilisateur associé à ce jeton n'existe plus. Veuillez vous reconnecter.");
        body.put("instance", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** Sans ces en-têtes, le navigateur masque le 401 (CORS) et le web ne purge pas sa session. */
    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return;
        }
        String resolved = corsConfiguration.checkOrigin(origin);
        if (resolved == null) {
            return;
        }
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, resolved);
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        if (Boolean.TRUE.equals(corsConfiguration.getAllowCredentials())) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }
}
