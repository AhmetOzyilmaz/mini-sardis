package com.mini.sardis.application.port.in.offer;

public interface CreateOfferUseCase {
    OfferResult execute(CreateOfferCommand command);
}
