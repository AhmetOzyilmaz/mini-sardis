package com.mini.sardis.application.service.plan;

import com.mini.sardis.application.exception.PlanNotFoundException;
import com.mini.sardis.application.port.in.plan.GetPlansUseCase;
import com.mini.sardis.application.port.in.plan.PlanResult;
import com.mini.sardis.application.port.out.SubscriptionPlanRepositoryPort;
import com.mini.sardis.domain.entity.SubscriptionPlan;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetPlansService implements GetPlansUseCase {

    private final SubscriptionPlanRepositoryPort planRepo;

    public GetPlansService(SubscriptionPlanRepositoryPort planRepo) {
        this.planRepo = planRepo;
    }

    @Override
    @Cacheable("plans")
    @Transactional(readOnly = true)
    public List<PlanResult> findAll() {
        return planRepo.findAllActive().stream().map(this::toResult).toList();
    }

    @Override
    @Cacheable(value = "plan", key = "#id")
    @Transactional(readOnly = true)
    public PlanResult findById(UUID id) {
        return planRepo.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new PlanNotFoundException(id));
    }

    private PlanResult toResult(SubscriptionPlan p) {
        return new PlanResult(p.getId(), p.getName(), p.getDescription(),
                p.getPrice().getAmount(), p.getPrice().getCurrency(),
                p.getDurationDays(), p.getTrialDays(), p.isActive());
    }
}
