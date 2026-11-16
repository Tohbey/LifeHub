package com.lifehub.finance.service;

import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.finance.model.dto.BudgetDTO;
import com.lifehub.finance.model.dto.CreateBudgetRequest;
import com.lifehub.finance.model.entity.Budget;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.repository.BudgetRepository;
import com.lifehub.finance.repository.CategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
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
    private final Clock clock;

    /** Checks membership, then reads one page and adds this month's spending to each budget. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BudgetDTO> getBudgets(UUID workspaceId, Pageable pageable) {
        access.require(workspaceId);
        YearMonth month = BudgetThresholds.period(clock.instant());
        return PageResponse.of(budgets.findByWorkspaceId(workspaceId, pageable), b -> toDto(b, month));
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
            return toDto(budget, BudgetThresholds.period(clock.instant()));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("This category already has a budget in " + request.currency() + ".");
        }
    }

    /** Maps a budget with what was spent against it in this month. */
    private BudgetDTO toDto(Budget b, YearMonth month) {
        var spent = transactions.spent(b.getWorkspaceId(), b.getCategoryId(), b.getCurrency(),
                BudgetThresholds.start(month), BudgetThresholds.end(month));
        return new BudgetDTO(b.getId(), b.getCategoryId(), b.getAmountLimit(), b.getCurrency(), month.atDay(1),
                BudgetThresholds.lastDay(month), spent);
    }
}
