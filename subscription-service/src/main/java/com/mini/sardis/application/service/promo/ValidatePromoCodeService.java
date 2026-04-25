package com.mini.sardis.application.service.promo;

import com.mini.sardis.application.exception.InvalidPromoCodeException;
import com.mini.sardis.application.port.in.promo.ValidatePromoCodeQuery;
import com.mini.sardis.application.port.in.promo.ValidatePromoCodeUseCase;
import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.PromoCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidatePromoCodeService implements ValidatePromoCodeUseCase {

    private final PromoCodeRepositoryPort promoCodeRepo;

    public ValidatePromoCodeService(PromoCodeRepositoryPort promoCodeRepo) {
        this.promoCodeRepo = promoCodeRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidatePromoCodeQuery validate(String code) {
        PromoCode promoCode = promoCodeRepo.findByCode(code.toUpperCase())
                .orElse(null);

        if (promoCode == null) {
            return new ValidatePromoCodeQuery(code, null, null, null, 0, false, "Promo code not found");
        }

        try {
            promoCode.validate();
            return new ValidatePromoCodeQuery(
                    promoCode.getCode(),
                    promoCode.getDiscountType(),
                    promoCode.getDiscountValue(),
                    promoCode.getMaxUses(),
                    promoCode.getCurrentUses(),
                    true,
                    "Promo code is valid"
            );
        } catch (InvalidPromoCodeException e) {
            return new ValidatePromoCodeQuery(
                    promoCode.getCode(),
                    promoCode.getDiscountType(),
                    promoCode.getDiscountValue(),
                    promoCode.getMaxUses(),
                    promoCode.getCurrentUses(),
                    false,
                    e.getMessage()
            );
        }
    }
}
