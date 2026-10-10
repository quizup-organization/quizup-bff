package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.response.NotificationPreferenceView;
import io.github.quizup.bff.infrastructure.in.api.response.NotificationView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationPage;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.query.NotificationQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Facade de l'inbox : lecture paginée, compteur, transitions de lecture, suppression, préférences. */
@Service
public class NotificationViewService {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    public NotificationViewService(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    public CompletableFuture<PageResponse<NotificationView>> list(String userId,
                                                                  boolean unreadOnly,
                                                                  int page,
                                                                  int size) {
        return queryGateway
                .query(new NotificationQuery.GetNotificationsQuery(userId, unreadOnly, page, size),
                        QueryResponseTypes.instanceOf(NotificationPage.class))
                .thenApply(result -> PageResponse.of(
                        result.content().stream().map(NotificationView::of).toList(),
                        page,
                        size,
                        result.totalElements()));
    }

    public CompletableFuture<Long> unreadCount(String userId) {
        return queryGateway
                .query(new NotificationQuery.CountUnreadNotificationsQuery(userId),
                        QueryResponseTypes.instanceOf(Long.class));
    }

    public CompletableFuture<Void> markRead(String userId, String notificationId) {
        // Pré-vérification : 404/403 propre plutôt qu'une AggregateNotFoundException non mappée.
        return queryGateway
                .query(new NotificationQuery.GetNotificationQuery(userId, notificationId),
                        QueryResponseTypes.instanceOf(Notification.class))
                .thenCompose(_ -> commandGateway
                        .send(new NotificationCommand.MarkNotificationReadCommand(notificationId, userId))
                        .thenAccept(_ -> {
                        }));
    }

    /** Repasse une notification en non lue (swipe inversé / menu desktop). */
    public CompletableFuture<Void> markUnread(String userId, String notificationId) {
        return queryGateway
                .query(new NotificationQuery.GetNotificationQuery(userId, notificationId),
                        QueryResponseTypes.instanceOf(Notification.class))
                .thenCompose(_ -> commandGateway
                        .send(new NotificationCommand.MarkNotificationUnreadCommand(notificationId, userId))
                        .thenAccept(_ -> {
                        }));
    }

    /** Suppression (hard delete). Même pré-vérification propriétaire que la lecture. */
    public CompletableFuture<Void> delete(String userId, String notificationId) {
        return queryGateway
                .query(new NotificationQuery.GetNotificationQuery(userId, notificationId),
                        QueryResponseTypes.instanceOf(Notification.class))
                .thenCompose(_ -> commandGateway
                        .send(new NotificationCommand.DeleteNotificationCommand(notificationId, userId))
                        .thenAccept(_ -> {
                        }));
    }

    public CompletableFuture<Void> markAllRead(String userId) {
        return commandGateway
                .send(new NotificationCommand.MarkAllNotificationsReadCommand(userId))
                .thenAccept(_ -> {
                });
    }

    /** Suppression de toute l'inbox (hard delete, fan-out côté quizup-notification). */
    public CompletableFuture<Void> deleteAll(String userId) {
        return commandGateway
                .send(new NotificationCommand.DeleteAllNotificationsCommand(userId))
                .thenAccept(_ -> {
                });
    }

    public CompletableFuture<List<NotificationPreferenceView>> preferences(String userId) {
        return queryGateway
                .query(new NotificationQuery.GetNotificationPreferencesQuery(userId),
                        QueryResponseTypes.multipleInstancesOf(NotificationPreference.class))
                .thenApply(preferences -> preferences.stream()
                        .map(preference -> new NotificationPreferenceView(
                                preference.category(), preference.enabled()))
                        .toList());
    }

    public CompletableFuture<Void> updatePreference(String userId,
                                                    NotificationCategory category,
                                                    boolean enabled) {
        return commandGateway
                .send(new NotificationCommand.UpdateNotificationPreferenceCommand(userId, category, enabled))
                .thenAccept(_ -> {
                });
    }
}
