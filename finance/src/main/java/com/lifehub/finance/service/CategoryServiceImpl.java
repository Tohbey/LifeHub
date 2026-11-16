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
import com.lifehub.finance.model.entity.Category;
import com.lifehub.finance.repository.CategoryRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CategoryServiceImpl implements CategoryService {

    private final WorkspaceAccess access;
    private final CategoryRepository categories;
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
}
