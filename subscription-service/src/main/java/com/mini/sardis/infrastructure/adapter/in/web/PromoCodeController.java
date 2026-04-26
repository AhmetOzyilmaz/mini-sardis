package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.promo.*;
import com.mini.sardis.infrastructure.adapter.in.web.dto.*;
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
@Tag(name = "Promo Codes", description = "Promo code management and validation")
@RequiredArgsConstructor
public class PromoCodeController {

    private final CreatePromoCodeUseCase createUseCase;
    private final ValidatePromoCodeUseCase validateUseCase;
    private final AssignPromoCodeUseCase assignUseCase;
    private final GetUserPromoCodesUseCase getUserPromoCodesUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Create a promo code (admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/admin/promo-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCodeResponse> create(@Valid @RequestBody CreatePromoCodeRequest request) {
        var promo = createUseCase.execute(new CreatePromoCodeCommand(
                request.code(),
                request.discountType(),
                request.discountValue(),
                request.maxUses(),
                request.validFrom(),
                request.validTo(),
                request.applicableMonths()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(PromoCodeResponse.from(promo));
    }

    @Operation(summary = "Validate a promo code (public). Pass durationMonths to check plan-specific eligibility.")
    @GetMapping("/api/v1/promo-codes/{code}/validate")
    public ResponseEntity<ValidatePromoCodeResponse> validate(
            @PathVariable String code,
            @RequestParam(required = false) Integer durationMonths) {
        return ResponseEntity.ok(ValidatePromoCodeResponse.from(
                validateUseCase.validate(code, durationMonths)));
    }

    @Operation(summary = "Assign a promo code to one or more users (admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/admin/promo-codes/{code}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assign(
            @PathVariable String code,
            @Valid @RequestBody AssignPromoCodeRequest request) {
        assignUseCase.execute(new AssignPromoCodeCommand(code, request.userIds()));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get promo codes assigned to the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/my-promo-codes")
    public ResponseEntity<List<UserPromoCodeResponse>> myPromoCodes(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        List<UserPromoCodeResponse> results = getUserPromoCodesUseCase.execute(userId)
                .stream().map(UserPromoCodeResponse::from).toList();
        return ResponseEntity.ok(results);
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.extractUserId(token);
    }
}
