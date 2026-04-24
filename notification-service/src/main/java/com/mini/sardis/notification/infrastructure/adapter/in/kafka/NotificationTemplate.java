package com.mini.sardis.notification.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.mini.sardis.notification.domain.value.NotificationChannel;

record NotificationTemplate(NotificationChannel channel, String subject, String body) {

    static NotificationTemplate forTopic(String topic, JsonNode node) {
        return switch (topic) {
            case "subscription.activated.v1" -> new NotificationTemplate(
                    NotificationChannel.EMAIL,
                    "Aboneliğiniz aktif / Your subscription is active",
                    "Aboneliğiniz başarıyla aktif edildi. Bir sonraki yenileme: "
                            + node.path("nextRenewalDate").asText("—"));

            case "subscription.cancelled.v1" -> new NotificationTemplate(
                    NotificationChannel.EMAIL,
                    "Aboneliğiniz iptal edildi / Subscription cancelled",
                    "Aboneliğiniz iptal edildi. Yeniden abone olmak için platformumuzu ziyaret edin.");

            case "subscription.failed.v1" -> new NotificationTemplate(
                    NotificationChannel.EMAIL,
                    "Ödeme başarısız / Payment failed",
                    "Ödemeniz alınamadı, aboneliğiniz oluşturulamadı. Lütfen kart bilgilerinizi kontrol edin.");

            case "subscription.renewed.v1" -> new NotificationTemplate(
                    NotificationChannel.EMAIL,
                    "Aboneliğiniz yenilendi / Subscription renewed",
                    "Aboneliğiniz başarıyla yenilendi. Bir sonraki yenileme: "
                            + node.path("nextRenewalDate").asText("—"));

            case "subscription.suspended.v1" -> new NotificationTemplate(
                    NotificationChannel.SMS,
                    "Ödeme alınamadı / Payment failed",
                    "Ödemeniz alınamadı, aboneliğiniz askıya alındı. Lütfen kart bilgilerinizi güncelleyin.");

            default -> new NotificationTemplate(
                    NotificationChannel.EMAIL,
                    "Bildirim / Notification",
                    "Aboneliğinizle ilgili bir güncelleme var.");
        };
    }
}
