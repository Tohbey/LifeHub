package com.lifehub.finance.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.finance.model.entity.BudgetAlert;
import com.lifehub.finance.model.entity.BudgetAlertId;

/**
 * Which budget thresholds have already been announced, so each is announced once per period. Callers lock the
 * budget row first ({@link BudgetRepository#findLockedByWorkspaceIdAndCategoryIdAndCurrency}), so checking and saving
 * an alert cannot race, and check with {@link #findLockedById}: a locking read sees alerts other requests have
 * just committed, which a plain read inside a REPEATABLE READ transaction may not.
 */
@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, BudgetAlertId> {

    /** The alert, read with a lock so the latest committed state is seen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BudgetAlert> findLockedById(BudgetAlertId id);

    @Modifying(flushAutomatically = true)
    @Query("delete from BudgetAlert a where a.id.budgetId = :budgetId")
    /** Forgets every alert of a budget, before the budget is deleted. */
    void deleteAllByBudgetId(UUID budgetId);

    @Modifying(flushAutomatically = true)
    @Query("delete from BudgetAlert a where a.id.budgetId = :budgetId and a.id.periodStart = :periodStart")
    /** Forgets a budget's alerts for one period, so they can fire again after the limit changes. */
    void deleteByBudgetIdAndPeriod(UUID budgetId, LocalDate periodStart);
}
