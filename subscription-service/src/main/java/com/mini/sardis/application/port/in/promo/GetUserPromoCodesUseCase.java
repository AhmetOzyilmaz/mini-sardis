package com.mini.sardis.application.port.in.promo;

import java.util.List;
import java.util.UUID;

public interface GetUserPromoCodesUseCase {
    List<UserPromoCodeResult> execute(UUID userId);
}
