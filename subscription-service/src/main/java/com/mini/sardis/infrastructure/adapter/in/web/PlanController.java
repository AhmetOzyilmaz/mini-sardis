package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.port.in.plan.GetPlansUseCase;
import com.mini.sardis.application.port.in.plan.PlanResult;
import com.mini.sardis.infrastructure.adapter.in.web.dto.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Subscription plan catalog (public)")
@RequiredArgsConstructor
public class PlanController {

    private final GetPlansUseCase getPlansUseCase;

    @Operation(summary = "List all active subscription plans")
    @GetMapping
    public ResponseEntity<List<PlanResponse>> findAll() {
        return ResponseEntity.ok(getPlansUseCase.findAll().stream().map(PlanResponse::from).toList());
    }

    @Operation(summary = "Get a plan by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(PlanResponse.from(getPlansUseCase.findById(id)));
    }
}
