package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.subscription.*;
import com.mini.sardis.infrastructure.adapter.in.web.dto.CreateSubscriptionRequest;
import com.mini.sardis.infrastructure.adapter.in.web.dto.SubscriptionResponse;
import com.mini.sardis.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription lifecycle management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SubscriptionController {

    private final CreateSubscriptionUseCase createUseCase;
    private final CancelSubscriptionUseCase cancelUseCase;
    private final GetSubscriptionUseCase getUseCase;
    private final ReactivateSubscriptionUseCase reactivateUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Create a new subscription (returns 202 while payment is pending)")
    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        SubscriptionResult result = createUseCase.execute(
                new CreateSubscriptionCommand(userId, request.planId(), request.promoCode(), request.paymentMethod()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SubscriptionResponse.from(result));
    }

    @Operation(summary = "Get subscription by ID")
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(SubscriptionResponse.from(getUseCase.findById(id)));
    }

    @Operation(summary = "Get all subscriptions for the authenticated user")
    @GetMapping("/my")
    public ResponseEntity<List<SubscriptionResponse>> getMy(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(getUseCase.findByUserId(userId)
                .stream().map(SubscriptionResponse::from).toList());
    }

    @Operation(summary = "List all subscriptions (admin only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> getAll() {
        return ResponseEntity.ok(getUseCase.findAll()
                .stream().map(SubscriptionResponse::from).toList());
    }

    @Operation(summary = "Cancel a subscription")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "user_request") String reason,
            @Parameter(description = "If true, cancellation is deferred until the end of the current billing period")
            @RequestParam(defaultValue = "false") boolean cancelAtPeriodEnd,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        cancelUseCase.execute(id, userId, reason, cancelAtPeriodEnd);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate a suspended or grace-period subscription")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivate(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        SubscriptionResult result = reactivateUseCase.execute(id, userId);
        return ResponseEntity.ok(SubscriptionResponse.from(result));
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.extractUserId(token);
    }
}
