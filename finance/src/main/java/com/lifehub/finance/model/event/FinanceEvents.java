package com.lifehub.finance.model.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.lifehub.starter.events.EventType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Events this service publishes. Must match deploy/docs/events.md (finance.events). Money is a string decimal. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FinanceEvents {

    public static final String TOPIC = "finance.events";

    public static final EventType TRANSACTION_CREATED = new EventType(TOPIC, "transaction.created", 1);
    public static final EventType TRANSACTION_UPDATED = new EventType(TOPIC, "transaction.updated", 1);
    public static final EventType TRANSACTION_DELETED = new EventType(TOPIC, "transaction.deleted", 1);
    public static final EventType BUDGET_THRESHOLD_EXCEEDED = new EventType(TOPIC, "budget.threshold.exceeded", 1);

    public record Split(UUID userId, String amount) {
    }

    public record TransactionCreated(UUID transactionId, String type, String description, String amount,
            String currency, UUID categoryId, UUID payerUserId, Instant occurredAt, List<Split> splits) {
    }

    public record TransactionUpdated(UUID transactionId, List<String> changedFields, String amount, String currency,
            List<Split> splits) {
    }

    public record TransactionDeleted(UUID transactionId) {
    }

    public record BudgetThresholdExceeded(UUID budgetId, UUID categoryId, LocalDate periodStart, LocalDate periodEnd,
            String limit, String spent, String currency, int thresholdPercent) {
    }
}
