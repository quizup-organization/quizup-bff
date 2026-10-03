package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.NotificationViewService;
import io.github.quizup.bff.infrastructure.in.api.request.PageParams;
import io.github.quizup.bff.infrastructure.in.api.response.NotificationView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.UnreadCountView;
import io.github.quizup.microservice.security.SecurityHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/notifications} — inbox personnelle : page, compteur non lus, transitions de lecture.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationViewService notificationViewService;

    public NotificationController(NotificationViewService notificationViewService) {
        this.notificationViewService = notificationViewService;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<PageResponse<NotificationView>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageParams.validate(page, size, MAX_PAGE_SIZE);
        return notificationViewService
                .list(SecurityHelper.getUserId(), unreadOnly, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/unread-count")
    public CompletableFuture<ResponseEntity<UnreadCountView>> unreadCount() {
        return notificationViewService
                .unreadCount(SecurityHelper.getUserId())
                .thenApply(count -> ResponseEntity.ok(new UnreadCountView(count)));
    }

    @PostMapping("/{notificationId}/read")
    public CompletableFuture<ResponseEntity<Void>> markRead(@PathVariable String notificationId) {
        return notificationViewService
                .markRead(SecurityHelper.getUserId(), notificationId)
                .thenApply(_ -> ResponseEntity.ok().build());
    }

    @PostMapping("/read-all")
    public CompletableFuture<ResponseEntity<Void>> markAllRead() {
        return notificationViewService
                .markAllRead(SecurityHelper.getUserId())
                .thenApply(_ -> ResponseEntity.ok().build());
    }
}
