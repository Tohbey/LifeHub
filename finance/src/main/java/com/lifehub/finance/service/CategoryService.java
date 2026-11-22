package com.lifehub.finance.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.finance.model.dto.CategoryDTO;
import com.lifehub.finance.model.dto.CreateCategoryRequest;
import com.lifehub.finance.model.dto.UpdateCategoryRequest;
import com.lifehub.starter.web.PageResponse;

/** Income and expense categories of a workspace. Every member can read and add them; admins rename and delete. */
public interface CategoryService {

    /** Pages through the workspace's categories. */
    PageResponse<CategoryDTO> getCategories(UUID workspaceId, Pageable pageable);

    /** Adds a category. 409 if one with the same type and name exists. */
    CategoryDTO createCategory(UUID workspaceId, CreateCategoryRequest request);

    /** Renames a category. Owners and admins only; 409 if the new name is taken. */
    CategoryDTO renameCategory(UUID workspaceId, UUID categoryId, UpdateCategoryRequest request);

    /** Deletes an unused category. Owners and admins only; 409 while a transaction or budget uses it. */
    void deleteCategory(UUID workspaceId, UUID categoryId);
}
