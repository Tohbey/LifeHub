package com.lifehub.shoppingmeals.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipeDTO(UUID id, UUID workspaceId, String name, Integer servings, String instructions,
        String sourceUrl, List<IngredientDTO> ingredients, UUID createdByUserId, Instant createdAt,
        Instant updatedAt) {
}
