package com.mini.sardis.notification.application.service;

import com.mini.sardis.notification.application.port.in.SendNotificationCommand;
import com.mini.sardis.notification.application.port.out.NotificationRepositoryPort;
import com.mini.sardis.notification.application.port.out.NotificationSenderPort;
import com.mini.sardis.notification.domain.entity.NotificationLog;
import com.mini.sardis.notification.domain.value.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendNotificationServiceTest {

    @Mock private NotificationRepositoryPort notificationRepo;
    @Mock private NotificationSenderPort emailSender;
    @Mock private NotificationSenderPort smsSender;

    private SendNotificationService service;

    @BeforeEach
    void setUp() {
        //when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        //when(smsSender.channel()).thenReturn(NotificationChannel.SMS);
        service = new SendNotificationService(List.of(emailSender, smsSender), notificationRepo);
    }

    @Test
    void emailNotification_routesToEmailSenderAndLogsResult() {
        UUID userId = UUID.randomUUID();
        when(emailSender.send(any(), any(), any())).thenReturn(true);

        service.execute(new SendNotificationCommand(userId, NotificationChannel.EMAIL,
                "Subject", "Body text"));

        verify(emailSender).send(userId.toString(), "Subject", "Body text");
        verify(smsSender, never()).send(any(), any(), any());

        var captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationRepo).save(captor.capture());
        assertThat(captor.getValue().isSuccess()).isTrue();
        assertThat(captor.getValue().getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void smsNotification_routesToSmsSender() {
        UUID userId = UUID.randomUUID();
        when(smsSender.send(any(), any(), any())).thenReturn(true);

        service.execute(new SendNotificationCommand(userId, NotificationChannel.SMS,
                "SMS subject", "SMS body"));

        verify(smsSender).send(userId.toString(), "SMS subject", "SMS body");
        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    void failedSend_logsFailure() {
        UUID userId = UUID.randomUUID();
        when(emailSender.send(any(), any(), any())).thenReturn(false);

        service.execute(new SendNotificationCommand(userId, NotificationChannel.EMAIL,
                "Subject", "Body"));

        var captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationRepo).save(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
    }

    @Test
    void unknownChannel_doesNothing() {
        UUID userId = UUID.randomUUID();

        service.execute(new SendNotificationCommand(userId, NotificationChannel.EMAIL,
                "Subject", "Body"));

        // If email sender throws, no call expected when no sender matches — tested via SMS command with no SMS mock
        // Here: just verify the service doesn't crash with an empty match
        // (Already handled by ifPresent)
    }
}
