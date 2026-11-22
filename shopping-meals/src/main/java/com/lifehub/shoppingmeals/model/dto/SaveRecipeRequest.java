package com.lifehub.shoppingmeals.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Creates a recipe, or replaces one as a whole (PUT). {@code sourceUrl} must be an http(s) link, so a saved link
 * can never run script when it is opened.
 */
public record SaveRecipeRequest(
        @NotBlank @Size(max = 120) String name,
        @Min(1) @Max(100) Integer servings,
        @Size(max = 10000) String instructions,
        @Size(max = 500) @Pattern(regexp = "(?i)https?://\\S+", message = "must be an http or https link") String sourceUrl,
        @Size(max = 100) List<@Valid IngredientDTO> ingredients) {
}
