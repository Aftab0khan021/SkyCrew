package com.skycrew.controller;

import com.skycrew.dto.NotificationPreferenceRequest;
import com.skycrew.dto.PagedResponse;
import com.skycrew.model.Notification;
import com.skycrew.model.NotificationPreference;
import com.skycrew.model.User;
import com.skycrew.repository.UserRepository;
import com.skycrew.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification history and preferences")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "View notification history (paginated)")
    public ResponseEntity<PagedResponse<Notification>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(pageable));
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get current user's notification preferences")
    public ResponseEntity<NotificationPreference> getPreferences(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(notificationService.getPreferences(user.getId()));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences",
               description = "Toggle email/SMS notifications and set phone number")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(notificationService.updatePreferences(user.getId(), request));
    }
}
