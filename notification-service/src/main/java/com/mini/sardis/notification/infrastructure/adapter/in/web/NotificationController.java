package com.mini.sardis.notification.infrastructure.adapter.in.web;

import com.mini.sardis.notification.application.port.out.NotificationRepositoryPort;
import com.mini.sardis.notification.domain.entity.NotificationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification history")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepositoryPort notificationRepo;

    @Operation(summary = "Get notification history for a user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getByUser(@PathVariable UUID userId) {
        List<NotificationResponse> responses = notificationRepo.findByUserId(userId)
                .stream().map(NotificationResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get all notification logs (admin)")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll() {
        List<NotificationResponse> responses = notificationRepo.findAll()
                .stream().map(NotificationResponse::from).toList();
        return ResponseEntity.ok(responses);
    }
}
