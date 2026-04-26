package com.mini.sardis.application.service.offer;

import com.mini.sardis.application.port.in.offer.CreateOfferCommand;
import com.mini.sardis.application.port.in.offer.CreateOfferUseCase;
import com.mini.sardis.application.port.in.offer.OfferResult;
import com.mini.sardis.application.port.out.OfferRepositoryPort;
import com.mini.sardis.domain.entity.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateOfferService implements CreateOfferUseCase {

    private final OfferRepositoryPort offerRepo;

    @Override
    @Transactional
    public OfferResult execute(CreateOfferCommand command) {
        Offer offer = Offer.create(
                command.name(), command.description(), command.planId(), command.promoCodeId(),
                command.targetType(), command.targetUserId(), command.targetPlanId(),
                command.validFrom(), command.validTo());
        return OfferResult.from(offerRepo.save(offer));
    }
}
