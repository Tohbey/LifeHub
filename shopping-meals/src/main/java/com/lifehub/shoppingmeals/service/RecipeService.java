package com.lifehub.shoppingmeals.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.shoppingmeals.model.dto.AddToShoppingListRequest;
import com.lifehub.shoppingmeals.model.dto.RecipeDTO;
import com.lifehub.shoppingmeals.model.dto.SaveRecipeRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.starter.web.PageResponse;

/** A workspace's recipes. Any member adds them; the author or an owner or admin edits or deletes them. */
public interface RecipeService {

    /** Recipes by name, optionally only those whose name contains {@code query}. */
    PageResponse<RecipeDTO> getRecipes(UUID workspaceId, String query, Pageable pageable);

    /** One recipe with its ingredients. */
    RecipeDTO getRecipe(UUID workspaceId, UUID recipeId);

    /** Adds a recipe. */
    RecipeDTO createRecipe(UUID workspaceId, SaveRecipeRequest request);

    /** Replaces a recipe and its ingredients. */
    RecipeDTO replaceRecipe(UUID workspaceId, UUID recipeId, SaveRecipeRequest request);

    /** Deletes a recipe; planned meals keep their title. */
    void deleteRecipe(UUID workspaceId, UUID recipeId);

    /** Adds every ingredient to a shopping list of the same workspace, returning the new items. */
    List<ShoppingItemDTO> addToShoppingList(UUID workspaceId, UUID recipeId, AddToShoppingListRequest request);
}
