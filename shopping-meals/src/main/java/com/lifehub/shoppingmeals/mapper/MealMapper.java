package com.lifehub.shoppingmeals.mapper;

import java.util.List;

import com.lifehub.shoppingmeals.model.dto.IngredientDTO;
import com.lifehub.shoppingmeals.model.dto.MealPlanEntryDTO;
import com.lifehub.shoppingmeals.model.dto.RecipeDTO;
import com.lifehub.shoppingmeals.model.entity.MealPlanEntry;
import com.lifehub.shoppingmeals.model.entity.Recipe;
import com.lifehub.shoppingmeals.model.entity.RecipeIngredient;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MealMapper {

    /** Maps a recipe and its ingredients, already in list order, to a DTO. */
    public static RecipeDTO toDto(Recipe r, List<RecipeIngredient> ingredients) {
        return new RecipeDTO(r.getId(), r.getWorkspaceId(), r.getName(), r.getServings(), r.getInstructions(),
                r.getSourceUrl(), ingredients.stream().map(i -> new IngredientDTO(i.getName(), i.getQuantity())).toList(),
                r.getCreatedByUserId(), r.getCreatedAt(), r.getUpdatedAt());
    }

    /** Maps a plan entry to a DTO. */
    public static MealPlanEntryDTO toDto(MealPlanEntry e) {
        return new MealPlanEntryDTO(e.getId(), e.getWorkspaceId(), e.getPlanDate(), e.getSlot(), e.getRecipeId(),
                e.getTitle(), e.getNote(), e.getCreatedByUserId(), e.getCreatedAt());
    }
}
