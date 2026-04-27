package com.mini.sardis.notification.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.mini.sardis.notification.domain.value.NotificationChannel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

record NotificationTemplate(NotificationChannel channel, String subject, String body) {

    // Map-based registry: adding a new event type requires no modification to forTopic() — OCP compliant.
    private static final Map<String, Function<JsonNode, List<NotificationTemplate>>> REGISTRY = Map.ofEntries(
            Map.entry("subscription.activated.v1", node -> {
                String renewal = node.path("nextRenewalDate").asText("—");
                return List.of(
                        email("Aboneliğiniz aktif / Your subscription is active",
                                "Aboneliğiniz başarıyla aktif edildi. Bir sonraki yenileme: " + renewal),
                        sms("Aboneliginiz aktif edildi. Sonraki yenileme: " + renewal));
            }),
            Map.entry("subscription.cancelled.v1", node -> List.of(
                    email("Aboneliğiniz iptal edildi / Subscription cancelled",
                            "Aboneliğiniz iptal edildi. Yeniden abone olmak için platformumuzu ziyaret edin."))),
            Map.entry("subscription.failed.v1", node -> List.of(
                    email("Ödeme başarısız / Payment failed",
                            "Ödemeniz alınamadı, aboneliğiniz oluşturulamadı. Lütfen kart bilgilerinizi kontrol edin."),
                    sms("Odeme baskisisz. Aboneliginiz olusturulamadi. Kart bilgilerinizi kontrol edin."))),
            Map.entry("subscription.renewed.v1", node -> {
                String renewal = node.path("nextRenewalDate").asText("—");
                return List.of(email("Aboneliğiniz yenilendi / Subscription renewed",
                        "Aboneliğiniz başarıyla yenilendi. Bir sonraki yenileme: " + renewal));
            }),
            Map.entry("subscription.suspended.v1", node -> List.of(
                    email("Ödeme alınamadı — Abonelik askıya alındı / Subscription suspended",
                            "Ödemeniz alınamadı ve aboneliğiniz askıya alındı. Lütfen kart bilgilerinizi güncelleyin."),
                    sms("Odeme alinamadi. Aboneliginiz askiya alindi. Kart bilgilerinizi guncelleyin."))),
            Map.entry("subscription.grace_period.v1", node -> {
                String graceEnd = node.path("gracePeriodEndDate").asText("—");
                return List.of(
                        email("Ödeme alınamadı — Grace Period başladı / Grace Period Started",
                                "Yenileme ödemeniz alınamadı. " + graceEnd + " tarihine kadar ödeme yapmazsanız aboneliğiniz iptal edilecektir."),
                        sms("Odeme alinamadi. " + graceEnd + " tarihine kadar odeme yapmazsiniz aboneliginiz iptal edilir."));
            }),
            Map.entry("refund.completed.v1", node -> {
                String amount = node.path("amount").asText("—");
                String currency = node.path("currency").asText("TRY");
                return List.of(
                        email("İade işleminiz tamamlandı / Refund processed",
                                "İade talebiniz başarıyla işlendi. " + amount + " " + currency + " tutarındaki iade hesabınıza aktarılacaktır."),
                        sms("Iade talebiniz islendi. " + amount + " " + currency + " iadeniz aktarilacaktir."));
            }),
            Map.entry("refund.failed.v1", node -> {
                String reason = node.path("reason").asText("unknown");
                return List.of(
                        email("İade işleminiz başarısız / Refund failed",
                                "İade talebiniz işlenemedi. Neden: " + reason + ". Destek ekibimizle iletişime geçin."));
            })
    );

    static List<NotificationTemplate> forTopic(String topic, JsonNode node) {
        return REGISTRY.getOrDefault(topic,
                n -> List.of(email("Bildirim / Notification", "Aboneliğinizle ilgili bir güncelleme var."))
        ).apply(node);
    }

    private static NotificationTemplate email(String subject, String body) {
        return new NotificationTemplate(NotificationChannel.EMAIL, subject, body);
    }

    private static NotificationTemplate sms(String body) {
        return new NotificationTemplate(NotificationChannel.SMS, "SMS", body);
    }
}
