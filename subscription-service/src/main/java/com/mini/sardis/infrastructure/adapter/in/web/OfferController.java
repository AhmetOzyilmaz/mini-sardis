package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.offer.CreateOfferCommand;
import com.mini.sardis.application.port.in.offer.CreateOfferUseCase;
import com.mini.sardis.application.port.in.offer.GetOffersUseCase;
import com.mini.sardis.infrastructure.adapter.in.web.dto.CreateOfferRequest;
import com.mini.sardis.infrastructure.adapter.in.web.dto.OfferResponse;
import com.mini.sardis.infrastructure.security.JwtTokenProvider;
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
@Tag(name = "Offers", description = "Personalized subscription offers")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OfferController {

    private final CreateOfferUseCase createUseCase;
    private final GetOffersUseCase getOffersUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Get eligible offers for the authenticated user")
    @GetMapping("/api/v1/offers")
    public ResponseEntity<List<OfferResponse>> getEligible(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(getOffersUseCase.findEligibleForUser(userId)
                .stream().map(OfferResponse::from).toList());
    }

    @Operation(summary = "Get offer by ID")
    @GetMapping("/api/v1/offers/{id}")
    public ResponseEntity<OfferResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(OfferResponse.from(getOffersUseCase.findById(id)));
    }

    @Operation(summary = "Create a new offer (admin only)")
    @PostMapping("/api/v1/admin/offers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody CreateOfferRequest request) {
        var result = createUseCase.execute(new CreateOfferCommand(
                request.name(), request.description(), request.planId(), request.promoCodeId(),
                request.targetType(), request.targetUserId(), request.targetPlanId(),
                request.validFrom(), request.validTo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferResponse.from(result));
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.extractUserId(token);
    }
}
