package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lifehub.finance.model.entity.Budget;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.model.entity.Transaction;
import com.lifehub.finance.model.event.FinanceEvents;
import com.lifehub.finance.repository.BudgetAlertRepository;
import com.lifehub.finance.repository.BudgetRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.starter.events.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

/**
 * After an expense is recorded or changed, checks its category's budget for that month and publishes
 * budget.threshold.exceeded for each threshold newly reached. Each threshold is announced once per budget
 * and month, even when requests race.
 */
@Component
@RequiredArgsConstructor
class BudgetMonitor {

    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final BudgetAlertRepository alerts;
    private final DomainEventPublisher events;

    /** Runs inside the caller's transaction, so the new expense is already counted. */
    void check(Transaction transaction, UUID actorUserId) {
        if (transaction.getType() != EntryType.EXPENSE || transaction.getCategoryId() == null) {
            return;
        }
        YearMonth month = BudgetThresholds.period(transaction.getOccurredAt());
        for (Budget budget : budgets.findByWorkspaceIdAndCategoryIdAndCurrency(transaction.getWorkspaceId(),
                transaction.getCategoryId(), transaction.getCurrency())) {
            BigDecimal spent = transactions.spent(budget.getWorkspaceId(), budget.getCategoryId(), budget.getCurrency(),
                    BudgetThresholds.start(month), BudgetThresholds.end(month));
            for (int percent : BudgetThresholds.reached(budget.getAmountLimit(), spent)) {
                if (alerts.recordOnce(budget.getId(), month.atDay(1), percent)) {
                    events.publish(FinanceEvents.BUDGET_THRESHOLD_EXCEEDED, budget.getWorkspaceId(), actorUserId,
                            new FinanceEvents.BudgetThresholdExceeded(budget.getId(), budget.getCategoryId(),
                                    month.atDay(1), BudgetThresholds.lastDay(month), Money.format(budget.getAmountLimit()),
                                    Money.format(spent), budget.getCurrency(), percent));
                }
            }
        }
    }
}
