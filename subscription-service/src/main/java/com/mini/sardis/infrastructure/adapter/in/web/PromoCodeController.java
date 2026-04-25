package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.promo.CreatePromoCodeCommand;
import com.mini.sardis.application.port.in.promo.CreatePromoCodeUseCase;
import com.mini.sardis.application.port.in.promo.ValidatePromoCodeUseCase;
import com.mini.sardis.infrastructure.adapter.in.web.dto.CreatePromoCodeRequest;
import com.mini.sardis.infrastructure.adapter.in.web.dto.PromoCodeResponse;
import com.mini.sardis.infrastructure.adapter.in.web.dto.ValidatePromoCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Promo Codes", description = "Promo code management and validation")
@RequiredArgsConstructor
public class PromoCodeController {

    private final CreatePromoCodeUseCase createUseCase;
    private final ValidatePromoCodeUseCase validateUseCase;

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
}
