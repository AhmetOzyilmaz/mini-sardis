package com.mini.sardis.application.service.transaction;

import com.mini.sardis.application.port.in.transaction.GetTransactionHistoryUseCase;
import com.mini.sardis.application.port.in.transaction.TransactionRecord;
import com.mini.sardis.application.port.out.SubscriptionRepositoryPort;
import com.mini.sardis.application.port.out.TransactionHistoryRepositoryPort;
import com.mini.sardis.domain.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTransactionHistoryService implements GetTransactionHistoryUseCase {

    private final TransactionHistoryRepositoryPort historyRepo;
    private final SubscriptionRepositoryPort subscriptionRepo;

    @Override
    public List<TransactionRecord> findBySubscriptionId(UUID subscriptionId) {
        return historyRepo.findByAggregateId(subscriptionId)
                .stream().map(this::toRecord).toList();
    }

    @Override
    public List<TransactionRecord> findByUserId(UUID userId) {
        List<UUID> subscriptionIds = subscriptionRepo.findByUserId(userId)
                .stream().map(s -> s.getId()).toList();
        if (subscriptionIds.isEmpty()) {
            return List.of();
        }
        return historyRepo.findByAggregateIds(subscriptionIds)
                .stream().map(this::toRecord).toList();
    }

    private TransactionRecord toRecord(OutboxEvent e) {
        String summary = e.getPayload().length() > 200
                ? e.getPayload().substring(0, 200) + "..."
                : e.getPayload();
        return new TransactionRecord(e.getId(), e.getAggregateId(), e.getEventType(), summary, e.getCreatedAt());
    }
}
