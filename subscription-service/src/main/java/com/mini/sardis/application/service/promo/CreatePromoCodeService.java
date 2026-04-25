package com.mini.sardis.application.service.promo;

import com.mini.sardis.application.port.in.promo.CreatePromoCodeCommand;
import com.mini.sardis.application.port.in.promo.CreatePromoCodeUseCase;
import com.mini.sardis.application.port.out.PromoCodeRepositoryPort;
import com.mini.sardis.domain.entity.PromoCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePromoCodeService implements CreatePromoCodeUseCase {

    private final PromoCodeRepositoryPort promoCodeRepo;

    public CreatePromoCodeService(PromoCodeRepositoryPort promoCodeRepo) {
        this.promoCodeRepo = promoCodeRepo;
    }

    @Override
    @Transactional
    public PromoCode execute(CreatePromoCodeCommand command) {
        PromoCode promoCode = PromoCode.create(
                command.code(),
                command.discountType(),
                command.discountValue(),
                command.maxUses(),
                command.validFrom(),
                command.validTo()
        );
        return promoCodeRepo.save(promoCode);
    }
}
