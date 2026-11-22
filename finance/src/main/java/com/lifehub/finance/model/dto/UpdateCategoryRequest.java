package com.lifehub.finance.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new name for a category. Its type (income or expense) cannot change. */
public record UpdateCategoryRequest(@NotBlank @Size(max = 60) String name) {
}
