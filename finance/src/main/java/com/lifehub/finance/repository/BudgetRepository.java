package com.lifehub.finance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.lifehub.finance.model.entity.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /** Pages through a workspace's budgets. */
    Page<Budget> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    /**
     * The budget an expense in this category and currency counts against (at most one), locked until the
     * transaction ends, so two expenses checking its thresholds at once take turns.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Budget> findLockedByWorkspaceIdAndCategoryIdAndCurrency(UUID workspaceId, UUID categoryId, String currency);

    /** Finds a budget only if it belongs to this workspace. */
    Optional<Budget> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /** True if any budget uses the category. */
    boolean existsByCategoryId(UUID categoryId);
}
