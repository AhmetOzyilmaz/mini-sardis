package com.mini.sardis.application.port.in.plan;

import java.util.List;
import java.util.UUID;

public interface GetPlansUseCase {
    List<PlanResult> findAll();
    PlanResult findById(UUID id);
}
