package com.mini.sardis.application.service.promo;

import com.mini.sardis.application.exception.InvalidPromoCodeException;
import com.mini.sardis.application.port.in.promo.ValidatePromoCodeQuery;
import com.mini.sardis.application.port.in.promo.ValidatePromoCodeUseCase;
import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.PromoCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ValidatePromoCodeService implements ValidatePromoCodeUseCase {

    private final PromoCodeRepositoryPort promoCodeRepo;

    @Override
    @Transactional(readOnly = true)
    public ValidatePromoCodeQuery validate(String code, Integer durationMonths) {
        PromoCode promoCode = promoCodeRepo.findByCode(code.toUpperCase()).orElse(null);

        if (promoCode == null) {
            return new ValidatePromoCodeQuery(code, null, null, null, 0, false, "Promo code not found", null);
        }

        try {
            if (durationMonths != null) {
                promoCode.validate(durationMonths);
            } else {
                promoCode.validate();
            }
            return new ValidatePromoCodeQuery(
                    promoCode.getCode(),
                    promoCode.getDiscountType(),
                    promoCode.getDiscountValue(),
                    promoCode.getMaxUses(),
                    promoCode.getCurrentUses(),
                    true,
                    "Promo code is valid",
                    promoCode.getApplicableMonths()
            );
        } catch (InvalidPromoCodeException e) {
            return new ValidatePromoCodeQuery(
                    promoCode.getCode(),
                    promoCode.getDiscountType(),
                    promoCode.getDiscountValue(),
                    promoCode.getMaxUses(),
                    promoCode.getCurrentUses(),
                    false,
                    e.getMessage(),
                    promoCode.getApplicableMonths()
            );
        }
    }
}
