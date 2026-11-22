package com.lifehub.finance.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.finance.model.dto.BudgetDTO;
import com.lifehub.finance.model.dto.CreateBudgetRequest;
import com.lifehub.finance.model.dto.UpdateBudgetRequest;
import com.lifehub.starter.web.PageResponse;

/** Monthly budgets per expense category. Every member can read them; owners and admins set them. */
public interface BudgetService {

    /** Pages through budgets, each with this month's spending. */
    PageResponse<BudgetDTO> getBudgets(UUID workspaceId, Pageable pageable);

    /** Creates a budget. 409 if the category already has one in that currency. */
    BudgetDTO createBudget(UUID workspaceId, CreateBudgetRequest request);

    /** Changes a budget's monthly limit; this month's alerts can fire again against the new limit. */
    BudgetDTO updateBudget(UUID workspaceId, UUID budgetId, UpdateBudgetRequest request);

    /** Deletes a budget and its alert history. Spending is not affected. */
    void deleteBudget(UUID workspaceId, UUID budgetId);
}
