package com.mini.sardis.notification.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.mini.sardis.notification.domain.value.NotificationChannel;

import java.util.List;

record NotificationTemplate(NotificationChannel channel, String subject, String body) {

    static List<NotificationTemplate> forTopic(String topic, JsonNode node) {
        String renewal = node.path("nextRenewalDate").asText("—");

        return switch (topic) {
            case "subscription.activated.v1" -> List.of(
                    email("Aboneliğiniz aktif / Your subscription is active",
                            "Aboneliğiniz başarıyla aktif edildi. Bir sonraki yenileme: " + renewal),
                    sms("Aboneliginiz aktif edildi. Sonraki yenileme: " + renewal));

            case "subscription.cancelled.v1" -> List.of(
                    email("Aboneliğiniz iptal edildi / Subscription cancelled",
                            "Aboneliğiniz iptal edildi. Yeniden abone olmak için platformumuzu ziyaret edin."));

            case "subscription.failed.v1" -> List.of(
                    email("Ödeme başarısız / Payment failed",
                            "Ödemeniz alınamadı, aboneliğiniz oluşturulamadı. Lütfen kart bilgilerinizi kontrol edin."),
                    sms("Odeme baskisisz. Aboneliginiz olusturulamadi. Kart bilgilerinizi kontrol edin."));

            case "subscription.renewed.v1" -> List.of(
                    email("Aboneliğiniz yenilendi / Subscription renewed",
                            "Aboneliğiniz başarıyla yenilendi. Bir sonraki yenileme: " + renewal));

            case "subscription.suspended.v1" -> List.of(
                    email("Ödeme alınamadı — Abonelik askıya alındı / Subscription suspended",
                            "Ödemeniz alınamadı ve aboneliğiniz askıya alındı. Lütfen kart bilgilerinizi güncelleyin."),
                    sms("Odeme alinamadi. Aboneliginiz askiya alindi. Kart bilgilerinizi guncelleyin."));

            default -> List.of(
                    email("Bildirim / Notification",
                            "Aboneliğinizle ilgili bir güncelleme var."));
        };
    }

    private static NotificationTemplate email(String subject, String body) {
        return new NotificationTemplate(NotificationChannel.EMAIL, subject, body);
    }

    private static NotificationTemplate sms(String body) {
        return new NotificationTemplate(NotificationChannel.SMS, "SMS", body);
    }
}
