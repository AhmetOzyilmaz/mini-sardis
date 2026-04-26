package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.transaction.GetTransactionHistoryUseCase;
import com.mini.sardis.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.mini.sardis.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Transactions", description = "Subscription event history")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TransactionController {

    private final GetTransactionHistoryUseCase historyUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Get event history for a specific subscription")
    @GetMapping("/api/v1/subscriptions/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getBySubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(historyUseCase.findBySubscriptionId(id)
                .stream().map(TransactionResponse::from).toList());
    }

    @Operation(summary = "Get all subscription events for the authenticated user")
    @GetMapping("/api/v1/transactions/my")
    public ResponseEntity<List<TransactionResponse>> getMy(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(historyUseCase.findByUserId(userId)
                .stream().map(TransactionResponse::from).toList());
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.extractUserId(token);
    }
}
