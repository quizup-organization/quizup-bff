package io.github.quizup.bff.infrastructure.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.bff.application.IdentityLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserExistsFilterTest {

    @Mock
    private IdentityLookup identityLookup;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CurrentUserExistsFilter filter() {
        return new CurrentUserExistsFilter(identityLookup, objectMapper);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughWhenUnauthenticated() throws Exception {
        MockHttpServletRequest request = request("/api/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void passesThroughForExistingUser() throws Exception {
        authenticate("user-1");
        when(identityLookup.exists("user-1")).thenReturn(CompletableFuture.completedFuture(true));
        MockHttpServletRequest request = request("/api/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsWith401WhenUserNoLongerExists() throws Exception {
        authenticate("ghost");
        when(identityLookup.exists("ghost")).thenReturn(CompletableFuture.completedFuture(false));
        MockHttpServletRequest request = request("/api/matchmaking/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("urn:quizup:authentification:unknownUser"));
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void failsOpenWhenIdentityLookupFails() throws Exception {
        authenticate("user-1");
        when(identityLookup.exists("user-1"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("identity down")));
        MockHttpServletRequest request = request("/api/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        authenticate("ghost");
        MockHttpServletRequest request = request("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    private static MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    private static void authenticate(String userId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", userId)
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
