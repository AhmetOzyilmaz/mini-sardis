package com.mini.sardis.payment.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.payment.application.port.in.HandleWebhookCommand;
import com.mini.sardis.payment.application.port.in.HandleWebhookUseCase;
import com.mini.sardis.common.security.WebhookSignatureVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@Tag(name = "Payment Webhook", description = "Receives async payment results from the provider")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final HandleWebhookUseCase handleWebhookUseCase;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Receive a payment webhook from the external provider")
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            HttpServletRequest httpRequest) throws IOException {

        byte[] bodyBytes = httpRequest.getInputStream().readAllBytes();
        String rawBody = new String(bodyBytes, StandardCharsets.UTF_8);

        signatureVerifier.verify(rawBody, signature);

        WebhookRequest request = objectMapper.readValue(rawBody, WebhookRequest.class);

        handleWebhookUseCase.execute(new HandleWebhookCommand(
                request.idempotencyKey(),
                request.externalRef(),
                request.success(),
                request.failureReason()
        ));
        return ResponseEntity.ok().build();
    }
}
