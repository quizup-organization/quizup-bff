package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.AccountDeletionService;
import io.github.quizup.bff.application.MeService;
import io.github.quizup.bff.application.ProfileUpdateService;
import io.github.quizup.bff.infrastructure.in.api.response.DuelStatsView;
import io.github.quizup.bff.infrastructure.in.api.response.MeView;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionView;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeControllerTest {

    private static final String USER_ID = "user-1";

    private final MeService meService = mock(MeService.class);
    private final AccountDeletionService accountDeletionService = mock(AccountDeletionService.class);
    private final ProfileUpdateService profileUpdateService = mock(ProfileUpdateService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeController(meService, accountDeletionService, profileUpdateService)).build();
        QuizUpPrincipal principal = new QuizUpPrincipal() {
            @Override
            public String getUserId() {
                return USER_ID;
            }

            @Override
            public String getEmail() {
                return "user@quizup.io";
            }
        };
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void me_returns_current_player_view() throws Exception {
        MeView view = new MeView(USER_ID, "user@quizup.io", "Alpha", null, "FR", null, null, null,
                Language.FR,
                new ProgressionView(120, 2, "Apprenti", 400, 6, List.of(), 200, 200, 0, true),
                new DuelStatsView(4, 3, 1, 0, 75, 160, 2, 3), 3, 5, false, false);
        when(meService.me(USER_ID)).thenReturn(CompletableFuture.completedFuture(view));

        MvcResult result = mockMvc.perform(get("/api/me"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.language").value("fr"))
                .andExpect(jsonPath("$.progression.level").value(2))
                .andExpect(jsonPath("$.premium").value(false));
    }
    @Test
    void completeOnboarding_returnsUpdatedMeView() throws Exception {
        MeView view = new MeView(USER_ID, "user@quizup.io", "Alpha", null, "FR", null, null, null,
                Language.FR,
                new ProgressionView(120, 2, "Apprenti", 400, 6, List.of(), 200, 200, 0, true),
                new DuelStatsView(4, 3, 1, 0, 75, 160, 2, 3), 3, 5, false, false);
        when(profileUpdateService.completeOnboarding(USER_ID))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(meService.isPremium()).thenReturn(false);
        when(meService.me(USER_ID, false)).thenReturn(CompletableFuture.completedFuture(view));

        MvcResult result = mockMvc.perform(post("/api/me/onboarding/complete"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
        verify(profileUpdateService).completeOnboarding(USER_ID);
    }

    @Test
    void requestDeletionCode_returnsAccepted() throws Exception {
        mockMvc.perform(post("/api/me/deletion-code"))
                .andExpect(status().isAccepted());

        verify(accountDeletionService).requestCode(USER_ID);
    }

    @Test
    void deleteAccount_returnsAccepted() throws Exception {
        mockMvc.perform(delete("/api/me")
                        .contentType("application/json")
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isAccepted());

        verify(accountDeletionService).delete(USER_ID, "123456");
    }
}
