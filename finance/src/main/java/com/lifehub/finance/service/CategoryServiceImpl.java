package com.lifehub.finance.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.finance.mapper.CategoryMapper;
import com.lifehub.finance.model.dto.CategoryDTO;
import com.lifehub.finance.model.dto.CreateCategoryRequest;
import com.lifehub.finance.model.dto.UpdateCategoryRequest;
import com.lifehub.finance.model.entity.Category;
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
class CategoryServiceImpl implements CategoryService {

    private final WorkspaceAccess access;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final Clock clock;

    /** Checks membership, then reads one page. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryDTO> getCategories(UUID workspaceId, Pageable pageable) {
        access.require(workspaceId);
        return PageResponse.of(categories.findByWorkspaceId(workspaceId, pageable), CategoryMapper::toDto);
    }

    /** Checks membership, then saves; the unique key turns a duplicate into 409. */
    @Override
    @Transactional
    public CategoryDTO createCategory(UUID workspaceId, CreateCategoryRequest request) {
        access.require(workspaceId);
        try {
            Category category = categories.saveAndFlush(
                    new Category(Ids.newId(), workspaceId, request.type(), request.name().strip(), clock.instant()));
            return CategoryMapper.toDto(category);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("A category with this name already exists.");
        }
    }

    /** Needs owner or admin; the unique key turns a taken name into 409. */
    @Override
    @Transactional
    public CategoryDTO renameCategory(UUID workspaceId, UUID categoryId, UpdateCategoryRequest request) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        Category category = load(workspaceId, categoryId);
        category.rename(request.name().strip());
        try {
            categories.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("A category with this name already exists.");
        }
        return CategoryMapper.toDto(category);
    }

    /** Needs owner or admin; a category still used by a transaction (even a deleted one) or budget is kept. */
    @Override
    @Transactional
    public void deleteCategory(UUID workspaceId, UUID categoryId) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        Category category = load(workspaceId, categoryId);
        if (transactions.existsByCategoryId(categoryId) || budgets.existsByCategoryId(categoryId)) {
            throw ApiException.conflict("This category is used by transactions or a budget. Rename it instead.");
        }
        categories.delete(category);
    }

    /** Loads a category of this workspace, or 404. */
    private Category load(UUID workspaceId, UUID categoryId) {
        return categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Category not found."));
    }
}
