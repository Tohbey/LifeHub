package com.lifehub.finance.service;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.finance.model.dto.BudgetDTO;
import com.lifehub.finance.model.dto.CreateBudgetRequest;
import com.lifehub.finance.model.dto.UpdateBudgetRequest;
import com.lifehub.finance.model.entity.Budget;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.repository.BudgetAlertRepository;
import com.lifehub.finance.repository.BudgetRepository;
import com.lifehub.finance.repository.CategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.membership.WorkspaceTimezones;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class BudgetServiceImpl implements BudgetService {

    private final WorkspaceAccess access;
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetAlertRepository alerts;
    private final WorkspaceTimezones zones;
    private final Clock clock;

    /** Checks membership, then reads one page and adds this month's spending to each budget. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BudgetDTO> getBudgets(UUID workspaceId, Pageable pageable) {
        access.require(workspaceId);
        ZoneId zone = zones.zoneOf(workspaceId);
        YearMonth month = BudgetThresholds.period(clock.instant(), zone);
        return PageResponse.of(budgets.findByWorkspaceId(workspaceId, pageable), b -> toDto(b, month, zone));
    }

    /** Needs owner or admin; the category must be one of this workspace's expense categories. */
    @Override
    @Transactional
    public BudgetDTO createBudget(UUID workspaceId, CreateBudgetRequest request) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        var category = categories.findByIdAndWorkspaceId(request.categoryId(), workspaceId)
                .orElseThrow(() -> ApiException.badRequest("Unknown category."));
        if (category.getType() != EntryType.EXPENSE) {
            throw ApiException.badRequest("Budgets are for expense categories.");
        }
        try {
            Budget budget = budgets.saveAndFlush(new Budget(Ids.newId(), workspaceId, request.categoryId(),
                    request.monthlyLimit(), request.currency(), clock.instant()));
            ZoneId zone = zones.zoneOf(workspaceId);
            return toDto(budget, BudgetThresholds.period(clock.instant(), zone), zone);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("This category already has a budget in " + request.currency() + ".");
        }
    }

    /** Needs owner or admin; clears this month's alerts so they can fire again against the new limit. */
    @Override
    @Transactional
    public BudgetDTO updateBudget(UUID workspaceId, UUID budgetId, UpdateBudgetRequest request) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        Budget budget = load(workspaceId, budgetId);
        ZoneId zone = zones.zoneOf(workspaceId);
        YearMonth month = BudgetThresholds.period(clock.instant(), zone);
        budget.changeLimit(request.monthlyLimit(), clock.instant());
        alerts.deleteByBudgetIdAndPeriod(budgetId, month.atDay(1));
        return toDto(budget, month, zone);
    }

    /** Needs owner or admin; removes the alert history first, then the budget. */
    @Override
    @Transactional
    public void deleteBudget(UUID workspaceId, UUID budgetId) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        Budget budget = load(workspaceId, budgetId);
        alerts.deleteAllByBudgetId(budgetId);
        budgets.delete(budget);
    }

    /** Loads a budget of this workspace, or 404. */
    private Budget load(UUID workspaceId, UUID budgetId) {
        return budgets.findByIdAndWorkspaceId(budgetId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Budget not found."));
    }

    /** Maps a budget with what was spent against it in this month (in the workspace's timezone). */
    private BudgetDTO toDto(Budget b, YearMonth month, ZoneId zone) {
        var spent = transactions.spent(b.getWorkspaceId(), b.getCategoryId(), b.getCurrency(),
                BudgetThresholds.start(month, zone), BudgetThresholds.end(month, zone));
        return new BudgetDTO(b.getId(), b.getCategoryId(), b.getAmountLimit(), b.getCurrency(), month.atDay(1),
                BudgetThresholds.lastDay(month), spent);
    }
}
