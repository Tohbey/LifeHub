package com.lifehub.finance.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lifehub.finance.model.entity.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /** Pages through a workspace's budgets. */
    Page<Budget> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    /** The budgets that an expense in this category and currency counts against (at most one). */
    List<Budget> findByWorkspaceIdAndCategoryIdAndCurrency(UUID workspaceId, UUID categoryId, String currency);
}
