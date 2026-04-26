package com.mini.sardis.application.port.in.transaction;

import java.util.List;
import java.util.UUID;

public interface GetTransactionHistoryUseCase {
    List<TransactionRecord> findBySubscriptionId(UUID subscriptionId);
    List<TransactionRecord> findByUserId(UUID userId);
}
