package com.ingoboka_api.v1.messaging.controllers;

import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.UserNotificationResponse;
import com.ingoboka_api.v1.messaging.services.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Email, SMS, push, and in-app notification records")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my notifications (paginated)")
    public ApiResponse<PageResponse<UserNotificationResponse>> listMine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Notifications retrieved", userNotificationService.listMyNotifications(page, size));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read")
    public ApiResponse<UserNotificationResponse> markRead(@PathVariable UUID notificationId) {
        return ApiResponse.ok("Notification updated", userNotificationService.markRead(notificationId));
    }
}
