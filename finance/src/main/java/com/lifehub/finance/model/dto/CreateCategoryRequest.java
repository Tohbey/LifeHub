package com.lifehub.finance.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.lifehub.finance.model.entity.EntryType;

public record CreateCategoryRequest(@NotNull EntryType type, @NotBlank @Size(max = 60) String name) {
}
