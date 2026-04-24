package com.mini.sardis.notification.application.port.in;

public interface SendNotificationUseCase {
    void execute(SendNotificationCommand command);
}
