package com.mini.sardis.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.webhook.secret:dev-webhook-secret-key}")
    private String webhookSecret;

    public void verify(String body, String signature) {
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook received without X-Signature header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Signature");
        }
        String expected = computeHmac(body);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Webhook signature mismatch");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
