package com.lifehub.shoppingmeals.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.shoppingmeals.mapper.MealMapper;
import com.lifehub.shoppingmeals.model.dto.AddToShoppingListRequest;
import com.lifehub.shoppingmeals.model.dto.CreateShoppingItemRequest;
import com.lifehub.shoppingmeals.model.dto.IngredientDTO;
import com.lifehub.shoppingmeals.model.dto.RecipeDTO;
import com.lifehub.shoppingmeals.model.dto.SaveRecipeRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.shoppingmeals.model.entity.Recipe;
import com.lifehub.shoppingmeals.model.entity.RecipeIngredient;
import com.lifehub.shoppingmeals.repository.RecipeIngredientRepository;
import com.lifehub.shoppingmeals.repository.RecipeRepository;
import com.lifehub.shoppingmeals.repository.ShoppingListRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class RecipeServiceImpl implements RecipeService {

    private final WorkspaceAccess access;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository ingredients;
    private final ShoppingListRepository lists;
    private final ShoppingItemService itemService;
    private final Clock clock;

    /** Checks membership, reads one page and loads all its ingredients in one query. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeDTO> getRecipes(UUID workspaceId, String query, Pageable pageable) {
        access.require(workspaceId);
        Page<Recipe> page = query == null || query.isBlank()
                ? recipes.findByWorkspaceId(workspaceId, pageable)
                : recipes.findByWorkspaceIdAndNameContainingIgnoreCase(workspaceId, query.strip(), pageable);
        Map<UUID, List<RecipeIngredient>> byRecipe = page.isEmpty() ? Map.of()
                : ingredients.findByRecipeIdInOrderByRecipeIdAscPositionAsc(page.map(Recipe::getId).getContent())
                        .stream().collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));
        return PageResponse.of(page, r -> MealMapper.toDto(r, byRecipe.getOrDefault(r.getId(), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDTO getRecipe(UUID workspaceId, UUID recipeId) {
        access.require(workspaceId);
        return toDto(load(workspaceId, recipeId));
    }

    /** Checks membership, then saves the recipe and its ingredients. */
    @Override
    @Transactional
    public RecipeDTO createRecipe(UUID workspaceId, SaveRecipeRequest request) {
        Membership caller = access.require(workspaceId);
        Recipe recipe = recipes.save(new Recipe(Ids.newId(), workspaceId, request.name().strip(), request.servings(),
                blankToNull(request.instructions()), blankToNull(request.sourceUrl()), caller.userId(), clock.instant()));
        return MealMapper.toDto(recipe, saveIngredients(recipe.getId(), request.ingredients()));
    }

    /** The author or an owner or admin replaces the details and the whole ingredient list. */
    @Override
    @Transactional
    public RecipeDTO replaceRecipe(UUID workspaceId, UUID recipeId, SaveRecipeRequest request) {
        Recipe recipe = loadForChange(workspaceId, recipeId);
        recipe.edit(request.name().strip(), request.servings(), blankToNull(request.instructions()),
                blankToNull(request.sourceUrl()), clock.instant());
        ingredients.deleteByRecipeId(recipeId);
        return MealMapper.toDto(recipe, saveIngredients(recipeId, request.ingredients()));
    }

    /** The author or an owner or admin deletes it; the database clears it from planned meals. */
    @Override
    @Transactional
    public void deleteRecipe(UUID workspaceId, UUID recipeId) {
        Recipe recipe = loadForChange(workspaceId, recipeId);
        ingredients.deleteByRecipeId(recipeId);
        recipes.delete(recipe);
    }

    /**
     * Adds each ingredient as an item through the normal item path, so every addition is checked, stored and
     * announced (shopping.item.added) exactly like one typed in by hand. Notification groups them per list.
     */
    @Override
    @Transactional
    public List<ShoppingItemDTO> addToShoppingList(UUID workspaceId, UUID recipeId, AddToShoppingListRequest request) {
        access.require(workspaceId);
        Recipe recipe = load(workspaceId, recipeId);
        lists.findByIdAndWorkspaceId(request.shoppingListId(), workspaceId)
                .orElseThrow(() -> ApiException.notFound("Shopping list not found."));
        List<ShoppingItemDTO> added = new ArrayList<>();
        for (RecipeIngredient i : ingredients.findByRecipeIdInOrderByRecipeIdAscPositionAsc(List.of(recipe.getId()))) {
            added.add(itemService.addItem(request.shoppingListId(),
                    new CreateShoppingItemRequest(i.getName(), i.getQuantity(), "For " + recipe.getName())));
        }
        if (added.isEmpty()) {
            throw ApiException.badRequest("This recipe has no ingredients to add.");
        }
        return added;
    }

    /** Loads a recipe of this workspace, or 404. */
    private Recipe load(UUID workspaceId, UUID recipeId) {
        return recipes.findByIdAndWorkspaceId(recipeId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Recipe not found."));
    }

    /** Loads a recipe the caller may change: theirs, or any when they are an owner or admin. */
    private Recipe loadForChange(UUID workspaceId, UUID recipeId) {
        Membership caller = access.require(workspaceId);
        Recipe recipe = load(workspaceId, recipeId);
        if (!recipe.getCreatedByUserId().equals(caller.userId()) && !caller.role().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden("Only the person who added this recipe, or an owner or admin, can change it.");
        }
        return recipe;
    }

    private RecipeDTO toDto(Recipe recipe) {
        return MealMapper.toDto(recipe, ingredients.findByRecipeIdInOrderByRecipeIdAscPositionAsc(List.of(recipe.getId())));
    }

    /** Saves the ingredient lines in order, skipping none. */
    private List<RecipeIngredient> saveIngredients(UUID recipeId, List<IngredientDTO> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<RecipeIngredient> saved = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            saved.add(new RecipeIngredient(Ids.newId(), recipeId, i, lines.get(i).name().strip(),
                    blankToNull(lines.get(i).quantity())));
        }
        return ingredients.saveAll(saved);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
