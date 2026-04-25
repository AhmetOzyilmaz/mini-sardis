package com.mini.sardis.payment.infrastructure.adapter.in.web;

import com.mini.sardis.payment.application.port.out.PaymentRepositoryPort;
import com.mini.sardis.payment.domain.entity.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment query endpoints")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepositoryPort paymentRepo;

    @Operation(summary = "Get payment history for a subscription")
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<PaymentResponse>> getBySubscription(@PathVariable UUID subscriptionId) {
        List<PaymentResponse> responses = paymentRepo.findBySubscriptionId(subscriptionId)
                .stream().map(PaymentResponse::from).toList();
        return ResponseEntity.ok(responses);
    }
}
