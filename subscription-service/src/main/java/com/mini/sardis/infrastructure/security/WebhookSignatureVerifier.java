package com.mini.sardis.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String webhookSecret;

    public WebhookSignatureVerifier(@Value("${app.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean verify(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Webhook signature header missing or malformed");
            return false;
        }

        try {
            String receivedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
            String expectedHex = computeHmacSha256(rawBody);
            return MessageDigest.isEqual(
                    HexFormat.of().parseHex(expectedHex),
                    HexFormat.of().parseHex(receivedHex)
            );
        } catch (Exception e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmacSha256(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM
        );
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
