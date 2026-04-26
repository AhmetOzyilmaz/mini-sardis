package com.mini.sardis.application.service.promo;

import com.mini.sardis.application.port.in.promo.GetUserPromoCodesUseCase;
import com.mini.sardis.application.port.in.promo.UserPromoCodeResult;
import com.mini.sardis.application.port.out.UserPromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.UserPromoCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserPromoCodesService implements GetUserPromoCodesUseCase {

    private final UserPromoCodeRepositoryPort userPromoCodeRepo;

    @Override
    @Transactional(readOnly = true)
    public List<UserPromoCodeResult> execute(UUID userId) {
        return userPromoCodeRepo.findByUserId(userId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    private UserPromoCodeResult toResult(UserPromoCode upc) {
        return new UserPromoCodeResult(
                upc.getId(), upc.getUserId(), upc.getCode(),
                upc.getAssignedAt(), upc.isUsed(), upc.getUsedAt());
    }
}
