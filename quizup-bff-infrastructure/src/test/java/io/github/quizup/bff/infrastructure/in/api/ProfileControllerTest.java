package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.ProfileViewService;
import io.github.quizup.bff.infrastructure.in.api.response.DuelStatsView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerProfileView;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionView;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.social.domain.command.UserFollowerCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerTest {

    private static final String USER_ID = "user-1";

    private final ProfileViewService profileViewService = mock(ProfileViewService.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(profileViewService, commandGateway)).build();
        TestSecurity.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void profile_returns_enriched_view() throws Exception {
        PlayerProfileView view = new PlayerProfileView("user-2", "Bravo", null, "FR", null,
                false, false, null,
                new ProgressionView(120, 2, "Apprenti", 400, 6, List.of()),
                new DuelStatsView(4, 3, 1, 0, 75, 160, 2, 3), 5, 7);
        when(profileViewService.profileView(USER_ID, "user-2"))
                .thenReturn(CompletableFuture.completedFuture(view));

        MvcResult result = mockMvc.perform(get("/api/profiles/user-2"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-2"))
                .andExpect(jsonPath("$.stats.wins").value(3))
                .andExpect(jsonPath("$.followersCount").value(5));
    }

    @Test
    void games_returns_page() throws Exception {
        when(profileViewService.games("user-2", null, null, 0, 20))
                .thenReturn(CompletableFuture.completedFuture(PageResponse.of(List.of(), 0, 20, 0)));

        MvcResult result = mockMvc.perform(get("/api/profiles/user-2/games"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unfollow_sends_command_with_actor() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture("user-1:user-2"));

        MvcResult result = mockMvc.perform(delete("/api/profiles/user-2/follow"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new UserFollowerCommand.UnfollowUserCommand("user-1:user-2", "user-1"));
    }

    @Test
    void updatePseudonym_sendsCommandWithActor() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(USER_ID));

        MvcResult result = mockMvc.perform(put("/api/profiles/user-1/pseudonym")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudonym\":\"Alicia\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new ProfileCommand.UpdateProfilePseudonymCommand(
                USER_ID, USER_ID, "Alicia"));
    }

    @Test
    void updateBio_sendsCommandWithActor() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(USER_ID));

        MvcResult result = mockMvc.perform(put("/api/profiles/user-1/bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Full stack\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new ProfileCommand.UpdateProfileBioCommand(
                USER_ID, USER_ID, "Full stack"));
    }

    @Test
    void updateCountry_sendsCommandWithActor() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(USER_ID));

        MvcResult result = mockMvc.perform(put("/api/profiles/user-1/country")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"FR\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new ProfileCommand.UpdateProfileCountryCommand(
                USER_ID, USER_ID, "FR"));
    }

    @Test
    void updateAvatarOptions_sendsCommandWithActor() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(USER_ID));

        MvcResult result = mockMvc.perform(put("/api/profiles/user-1/avatar-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarOptions\":\"{\\\"hair\\\":\\\"full\\\"}\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new ProfileCommand.UpdateProfileAvatarCommand(
                USER_ID, USER_ID, "{\"hair\":\"full\"}"));
    }

    @Test
    void updateLanguage_sendsCommandWithActor() throws Exception {
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(USER_ID));

        MvcResult result = mockMvc.perform(put("/api/profiles/user-1/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"en\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        verify(commandGateway).send(new ProfileCommand.UpdateProfileLanguageCommand(
                USER_ID, USER_ID, Language.EN));
    }

    @Test
    void updateLanguage_withUnsupportedLanguage_isRejected() throws Exception {
        mockMvc.perform(put("/api/profiles/user-1/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"de\"}"))
                .andExpect(status().isBadRequest());

        verify(commandGateway, never()).send(any());
    }
}
