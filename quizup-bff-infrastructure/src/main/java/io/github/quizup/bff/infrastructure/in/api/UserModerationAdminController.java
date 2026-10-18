package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.application.UserModerationAdminService;
import io.github.quizup.bff.application.UserReportService;
import io.github.quizup.bff.infrastructure.in.api.request.ReinstateUserRequest;
import io.github.quizup.bff.infrastructure.in.api.request.ResolveUserReportsRequest;
import io.github.quizup.bff.infrastructure.in.api.request.SuspendUserRequest;
import io.github.quizup.bff.infrastructure.in.api.response.AdminReportedUserSummaryView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserDetailView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserModerationSummaryView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserModerationView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserReportResolutionView;
import io.github.quizup.bff.infrastructure.in.api.response.AdminUserSearchResultView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.identity.domain.model.UserReportSort;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.model.UserReportSummaryFilter;
import io.github.quizup.microservice.security.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /api/admin} — modération des comptes : file de signalements, recherche, dossier,
 * suspension et réintégration. Surface réservée à {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class UserModerationAdminController {

    private final UserModerationAdminService adminService;
    private final UserReportService userReportService;

    public UserModerationAdminController(UserModerationAdminService adminService,
                                         UserReportService userReportService) {
        this.adminService = adminService;
        this.userReportService = userReportService;
    }

    @GetMapping("/user-reports")
    public CompletableFuture<ResponseEntity<PageResponse<AdminReportedUserSummaryView>>> queue(
            @RequestParam(defaultValue = "OPEN") UserReportStatus status,
            @RequestParam(required = false) Boolean suspended,
            @RequestParam(defaultValue = "RECENT_REPORT") UserReportSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserReportSummaryFilter filter = new UserReportSummaryFilter(status, suspended);
        return adminService.queue(filter, sort, safePage(page), safeSize(size))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/user-reports/summary")
    public CompletableFuture<ResponseEntity<AdminUserModerationSummaryView>> summary() {
        return adminService.summary().thenApply(ResponseEntity::ok);
    }

    @GetMapping("/users/search")
    public CompletableFuture<ResponseEntity<List<AdminUserSearchResultView>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return adminService.search(q, limit).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/users/{userId}")
    public CompletableFuture<ResponseEntity<AdminUserDetailView>> detail(@PathVariable String userId) {
        return adminService.detail(userId).thenApply(ResponseEntity::ok);
    }

    @PostMapping("/users/{userId}/suspend")
    public CompletableFuture<ResponseEntity<AdminUserModerationView>> suspend(
            @PathVariable String userId,
            @Valid @RequestBody SuspendUserRequest request) {
        return adminService.suspend(userId, SecurityHelper.getUserId(), request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/users/{userId}/reinstate")
    public CompletableFuture<ResponseEntity<AdminUserModerationView>> reinstate(
            @PathVariable String userId,
            @Valid @RequestBody(required = false) ReinstateUserRequest request) {
        return adminService.reinstate(userId, SecurityHelper.getUserId(), request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/user-reports/{reportId}/resolve")
    public CompletableFuture<ResponseEntity<AdminUserReportResolutionView>> resolveReport(
            @PathVariable String reportId,
            @Valid @RequestBody ResolveUserReportsRequest request) {
        return userReportService.resolveAdmin(reportId, SecurityHelper.getUserId(), request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/users/{userId}/reports/resolve")
    public CompletableFuture<ResponseEntity<AdminUserReportResolutionView>> resolveReports(
            @PathVariable String userId,
            @Valid @RequestBody ResolveUserReportsRequest request) {
        return userReportService.resolveAdminAll(userId, SecurityHelper.getUserId(), request)
                .thenApply(ResponseEntity::ok);
    }

    private static int safeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private static int safePage(int page) {
        return Math.max(page, 0);
    }
}
