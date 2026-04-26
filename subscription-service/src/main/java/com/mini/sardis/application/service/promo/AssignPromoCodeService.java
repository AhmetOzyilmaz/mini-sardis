package com.mini.sardis.application.service.promo;

import com.mini.sardis.application.exception.InvalidPromoCodeException;
import com.mini.sardis.application.port.in.promo.AssignPromoCodeCommand;
import com.mini.sardis.application.port.in.promo.AssignPromoCodeUseCase;
import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.application.port.out.UserPromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.PromoCode;
import com.mini.sardis.domain.entity.UserPromoCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignPromoCodeService implements AssignPromoCodeUseCase {

    private final PromoCodeRepositoryPort promoCodeRepo;
    private final UserPromoCodeRepositoryPort userPromoCodeRepo;

    @Override
    @Transactional
    public void execute(AssignPromoCodeCommand command) {
        PromoCode promo = promoCodeRepo.findByCode(command.code().toUpperCase())
                .filter(PromoCode::isActive)
                .orElseThrow(() -> new InvalidPromoCodeException(
                        "Promo code '" + command.code() + "' not found or inactive"));

        List<UserPromoCode> assignments = command.userIds().stream()
                .distinct()
                .filter(userId -> userPromoCodeRepo
                        .findByUserIdAndPromoCodeId(userId, promo.getId()).isEmpty())
                .map(userId -> UserPromoCode.assign(userId, promo.getId(), promo.getCode()))
                .toList();

        userPromoCodeRepo.saveAll(assignments);
        log.info("Assigned promo code '{}' to {} users", promo.getCode(), assignments.size());
    }
}
