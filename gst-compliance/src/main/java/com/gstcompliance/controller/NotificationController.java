package com.gstcompliance.controller;

import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.model.Notification;
import com.gstcompliance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        String email = authentication.getName();
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUserNotifications(email).stream()
                    .filter(n -> n.getStatus() == Notification.NotificationStatus.UNREAD)
                    .toList();
        } else {
            notifications = notificationService.getUserNotifications(email);
        }
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        notificationService.markAsRead(id, email);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        String email = authentication.getName();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        notificationService.deleteNotification(id, email);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}
