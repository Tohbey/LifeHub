package com.lifehub.finance.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.finance.model.dto.CategoryDTO;
import com.lifehub.finance.model.dto.CreateCategoryRequest;
import com.lifehub.starter.web.PageResponse;

/** Income and expense categories of a workspace. Every member can read and add them. */
public interface CategoryService {

    /** Pages through the workspace's categories. */
    PageResponse<CategoryDTO> getCategories(UUID workspaceId, Pageable pageable);

    /** Adds a category. 409 if one with the same type and name exists. */
    CategoryDTO createCategory(UUID workspaceId, CreateCategoryRequest request);
}
