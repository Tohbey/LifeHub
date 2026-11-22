package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lifehub.finance.model.entity.Budget;
import com.lifehub.finance.model.entity.BudgetAlert;
import com.lifehub.finance.model.entity.BudgetAlertId;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.model.entity.Transaction;
import com.lifehub.finance.model.event.FinanceEvents;
import com.lifehub.finance.repository.BudgetAlertRepository;
import com.lifehub.finance.repository.BudgetRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.membership.WorkspaceTimezones;

import lombok.RequiredArgsConstructor;

/**
 * After an expense is recorded or changed, checks its category's budget for that month (in the workspace's
 * timezone) and publishes
 * budget.threshold.exceeded for each threshold newly reached. Each threshold is announced once per budget
 * and month, even when requests race: the budget row is locked while its thresholds are checked.
 */
@Component
@RequiredArgsConstructor
class BudgetMonitor {

    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final BudgetAlertRepository alerts;
    private final DomainEventPublisher events;
    private final WorkspaceTimezones zones;
    private final Clock clock;

    /** Runs inside the caller's transaction, so the new expense is already counted. */
    void check(Transaction transaction, UUID actorUserId) {
        if (transaction.getType() != EntryType.EXPENSE || transaction.getCategoryId() == null) {
            return;
        }
        ZoneId zone = zones.zoneOf(transaction.getWorkspaceId());
        YearMonth month = BudgetThresholds.period(transaction.getOccurredAt(), zone);
        for (Budget budget : budgets.findLockedByWorkspaceIdAndCategoryIdAndCurrency(transaction.getWorkspaceId(),
                transaction.getCategoryId(), transaction.getCurrency())) {
            BigDecimal spent = transactions.lockBudgetExpenses(budget.getWorkspaceId(), budget.getCategoryId(),
                            budget.getCurrency(), BudgetThresholds.start(month, zone), BudgetThresholds.end(month, zone))
                    .stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            for (int percent : BudgetThresholds.reached(budget.getAmountLimit(), spent)) {
                BudgetAlertId alertId = new BudgetAlertId(budget.getId(), month.atDay(1), percent);
                // The budget row is locked, so no other request can save this alert between the check and the save.
                if (alerts.findLockedById(alertId).isEmpty()) {
                    alerts.save(new BudgetAlert(alertId, clock.instant()));
                    events.publish(FinanceEvents.BUDGET_THRESHOLD_EXCEEDED, budget.getWorkspaceId(), actorUserId,
                            new FinanceEvents.BudgetThresholdExceeded(budget.getId(), budget.getCategoryId(),
                                    month.atDay(1), BudgetThresholds.lastDay(month), Money.format(budget.getAmountLimit()),
                                    Money.format(spent), budget.getCurrency(), percent));
                }
            }
        }
    }
}
