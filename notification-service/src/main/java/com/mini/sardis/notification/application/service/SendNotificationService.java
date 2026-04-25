package com.mini.sardis.notification.application.service;

import com.mini.sardis.notification.application.port.in.SendNotificationCommand;
import com.mini.sardis.notification.application.port.in.SendNotificationUseCase;
import com.mini.sardis.notification.application.port.out.NotificationRepositoryPort;
import com.mini.sardis.notification.application.port.out.NotificationSenderPort;
import com.mini.sardis.notification.domain.entity.NotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SendNotificationService implements SendNotificationUseCase {

    private final List<NotificationSenderPort> senders;
    private final NotificationRepositoryPort notificationRepo;

    @Override
    @Transactional
    public void execute(SendNotificationCommand command) {
        senders.stream()
                .filter(s -> s.channel() == command.channel())
                .findFirst()
                .ifPresent(sender -> {
                    boolean success = sender.send(
                            command.userId().toString(),
                            command.subject(),
                            command.body());
                    NotificationLog log = NotificationLog.create(
                            command.userId(), command.channel(),
                            command.subject(), command.body(), success);
                    notificationRepo.save(log);
                });
    }
}
