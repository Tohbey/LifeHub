package com.lifehub.shoppingmeals.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One ingredient line, in requests and responses. */
public record IngredientDTO(@NotBlank @Size(max = 120) String name, @Size(max = 40) String quantity) {
}
