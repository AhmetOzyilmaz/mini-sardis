package com.mini.sardis.notification.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.notification.BaseComponentTest;
import com.mini.sardis.notification.application.port.in.SendNotificationCommand;
import com.mini.sardis.notification.application.port.in.SendNotificationUseCase;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import com.mini.sardis.notification.infrastructure.adapter.out.jpa.JpaNotificationRepository;
import com.mini.sardis.notification.infrastructure.adapter.out.jpa.NotificationLogJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationApiComponentTest extends BaseComponentTest {

    @Autowired JpaNotificationRepository notificationRepo;
    @Autowired SendNotificationUseCase sendNotificationUseCase;
    @Autowired ObjectMapper objectMapper;

    @Test
    void getByUser_emptyForUnknownUser() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications/user/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void getByUser_returnsNotificationsForUser() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationRepo.save(buildLog(userId, NotificationChannel.EMAIL, true));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications/user/" + userId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("channel").asText()).isEqualTo("EMAIL");
        assertThat(body.get(0).get("success").asBoolean()).isTrue();
    }

    @Test
    void getByUser_emailAndSms_bothReturned() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationRepo.save(buildLog(userId, NotificationChannel.EMAIL, true));
        notificationRepo.save(buildLog(userId, NotificationChannel.SMS, true));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications/user/" + userId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.findValuesAsText("channel")).containsExactlyInAnyOrder("EMAIL", "SMS");
    }

    @Test
    void getAll_returnsAllNotifications() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        notificationRepo.save(buildLog(userA, NotificationChannel.EMAIL, true));
        notificationRepo.save(buildLog(userB, NotificationChannel.SMS, false));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void sendNotification_emailChannel_persistsLog() throws Exception {
        UUID userId = UUID.randomUUID();
        sendNotificationUseCase.execute(new SendNotificationCommand(
                userId, NotificationChannel.EMAIL, "Test Subject", "Test body"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications/user/" + userId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("subject").asText()).isEqualTo("Test Subject");
        assertThat(body.get(0).get("channel").asText()).isEqualTo("EMAIL");
        assertThat(body.get(0).get("success").asBoolean()).isTrue();
    }

    @Test
    void sendNotification_smsChannel_persistsLog() throws Exception {
        UUID userId = UUID.randomUUID();
        sendNotificationUseCase.execute(new SendNotificationCommand(
                userId, NotificationChannel.SMS, "SMS Subject", "Kısa mesaj içeriği"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications/user/" + userId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("channel").asText()).isEqualTo("SMS");
        assertThat(body.get(0).get("success").asBoolean()).isTrue();
    }

    @Test
    void getByUser_isolatedFromOtherUsers() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        notificationRepo.save(buildLog(userA, NotificationChannel.EMAIL, true));
        notificationRepo.save(buildLog(userB, NotificationChannel.SMS, true));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/notifications/user/" + userA, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("channel").asText()).isEqualTo("EMAIL");
    }

    private NotificationLogJpaEntity buildLog(UUID userId, NotificationChannel channel, boolean success) {
        return NotificationLogJpaEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .channel(channel)
                .subject("Test notification")
                .body("Test body content")
                .success(success)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
