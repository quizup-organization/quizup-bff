package io.github.quizup.bff.application;

import io.github.quizup.notification.domain.event.NotificationEvent;
import io.github.quizup.notification.domain.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebPushDispatcherTest {

    private static final String USER_ID = "user-1";
    private static final String ENDPOINT = "endpoint-1";

    private final PushSubscriptionService subscriptionService = mock(PushSubscriptionService.class);
    private final WebPushGateway pushGateway = mock(WebPushGateway.class);
    private final ProfileLookup profiles = mock(ProfileLookup.class);
    private final WebPushDispatcher dispatcher =
            new WebPushDispatcher(subscriptionService, pushGateway, profiles);

    private final PushSubscription subscription = new PushSubscription(
            ENDPOINT, USER_ID, "p256dh", "auth", "Chrome", Instant.now(), Instant.now());

    @BeforeEach
    void setUp() {
        when(subscriptionService.subscriptionsOf(USER_ID)).thenReturn(List.of(subscription));
        when(profiles.get(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(pushGateway.send(any(), any())).thenReturn(WebPushGateway.SendStatus.DELIVERED);
    }

    @Test
    void invitation_path_targets_the_lobby() {
        dispatcher.dispatch(event(NotificationType.LOBBY_INVITATION, "lobby-1", "actor-1"));

        WebPushMessage message = capturedMessage();
        assertThat(message.type()).isEqualTo("LOBBY_INVITATION");
        assertThat(message.path()).isEqualTo("/lobbies/lobby-1");
        assertThat(message.sourceId()).isEqualTo("lobby-1");
    }

    @Test
    void follow_path_targets_the_actor_profile() {
        dispatcher.dispatch(event(NotificationType.FOLLOW, "follow-1", "actor-1"));

        assertThat(capturedMessage().path()).isEqualTo("/players/actor-1");
    }

    @Test
    void outcome_path_targets_the_inbox() {
        dispatcher.dispatch(event(NotificationType.LOBBY_ACCEPTED, "lobby-1", "actor-1"));

        assertThat(capturedMessage().path()).isEqualTo("/notifications");
    }

    @Test
    void gone_subscription_is_pruned() {
        when(pushGateway.send(any(), any())).thenReturn(WebPushGateway.SendStatus.GONE);

        dispatcher.dispatch(event(NotificationType.LOBBY_INVITATION, "lobby-1", "actor-1"));

        verify(subscriptionService).unregister(USER_ID, ENDPOINT);
    }

    @Test
    void no_subscription_skips_the_gateway() {
        when(subscriptionService.subscriptionsOf(USER_ID)).thenReturn(List.of());

        dispatcher.dispatch(event(NotificationType.LOBBY_INVITATION, "lobby-1", "actor-1"));

        verifyNoInteractions(pushGateway);
    }

    @Test
    void gateway_failure_is_swallowed() {
        when(pushGateway.send(any(), any())).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> dispatcher.dispatch(event(NotificationType.LOBBY_INVITATION, "lobby-1", "actor-1")))
                .doesNotThrowAnyException();
    }

    private WebPushMessage capturedMessage() {
        ArgumentCaptor<WebPushMessage> captor = ArgumentCaptor.forClass(WebPushMessage.class);
        verify(pushGateway).send(eq(subscription), captor.capture());
        return captor.getValue();
    }

    private static NotificationEvent.NotificationCreatedEvent event(NotificationType type,
                                                                    String sourceId,
                                                                    String actorId) {
        return new NotificationEvent.NotificationCreatedEvent(
                "notification-1",
                USER_ID,
                type,
                actorId,
                sourceId,
                "topic-1",
                null,
                Instant.parse("2026-01-01T11:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"));
    }
}
