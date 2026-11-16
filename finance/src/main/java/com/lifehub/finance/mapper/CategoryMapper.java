package com.lifehub.finance.mapper;

import com.lifehub.finance.model.dto.CategoryDTO;
import com.lifehub.finance.model.entity.Category;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategoryMapper {

    /** Maps a category entity to its DTO. */
    public static CategoryDTO toDto(Category category) {
        return new CategoryDTO(category.getId(), category.getType(), category.getName());
    }
}
