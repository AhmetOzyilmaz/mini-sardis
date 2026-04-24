package com.mini.sardis.payment.infrastructure.adapter.in.web;

import com.mini.sardis.payment.application.port.in.HandleWebhookCommand;
import com.mini.sardis.payment.application.port.in.HandleWebhookUseCase;
import com.mini.sardis.payment.infrastructure.security.WebhookSignatureVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@Tag(name = "Payment Webhook", description = "Receives async payment results from the provider")
public class PaymentWebhookController {

    private final HandleWebhookUseCase handleWebhookUseCase;
    private final WebhookSignatureVerifier signatureVerifier;

    public PaymentWebhookController(HandleWebhookUseCase handleWebhookUseCase,
                                     WebhookSignatureVerifier signatureVerifier) {
        this.handleWebhookUseCase = handleWebhookUseCase;
        this.signatureVerifier = signatureVerifier;
    }

    @Operation(summary = "Receive a payment webhook from the external provider")
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @Valid @RequestBody WebhookRequest request,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        signatureVerifier.verify(rawBody, signature);

        handleWebhookUseCase.execute(new HandleWebhookCommand(
                request.idempotencyKey(),
                request.externalRef(),
                request.success(),
                request.failureReason()
        ));
        return ResponseEntity.ok().build();
    }
}
