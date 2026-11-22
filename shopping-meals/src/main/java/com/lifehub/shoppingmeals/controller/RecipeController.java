package com.lifehub.shoppingmeals.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.shoppingmeals.model.dto.AddToShoppingListRequest;
import com.lifehub.shoppingmeals.model.dto.RecipeDTO;
import com.lifehub.shoppingmeals.model.dto.SaveRecipeRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.shoppingmeals.service.RecipeService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Recipes", description = "A workspace's recipes, and adding their ingredients to a shopping list.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/recipes")
@RequiredArgsConstructor
class RecipeController {

    private final RecipeService recipeService;

    /** GET /api/v1/workspaces/{id}/recipes: recipes by name; {@code ?q=} filters by name. */
    @Operation(summary = "List recipes", description = "Recipes by name, with their ingredients. q filters by name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of recipes"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Membership could not be checked right now; try again shortly", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<RecipeDTO> getRecipes(@PathVariable UUID workspaceId, @RequestParam(required = false) String q,
            @PageableDefault(sort = "name") Pageable pageable) {
        return recipeService.getRecipes(workspaceId, q, pageable);
    }

    /** GET /api/v1/workspaces/{id}/recipes/{recipeId}. */
    @Operation(summary = "Get a recipe")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The recipe"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Recipe or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{recipeId}")
    RecipeDTO getRecipe(@PathVariable UUID workspaceId, @PathVariable UUID recipeId) {
        return recipeService.getRecipe(workspaceId, recipeId);
    }

    /** POST /api/v1/workspaces/{id}/recipes: any member adds a recipe. */
    @Operation(summary = "Add a recipe", description = "Any member. sourceUrl must be an http or https link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recipe added",
                    content = @Content(schema = @Schema(implementation = RecipeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RecipeDTO createRecipe(@PathVariable UUID workspaceId, @Valid @RequestBody SaveRecipeRequest request) {
        return recipeService.createRecipe(workspaceId, request);
    }

    /** PUT /api/v1/workspaces/{id}/recipes/{recipeId}: replaces the recipe and its ingredient list. */
    @Operation(summary = "Replace a recipe",
            description = "Replaces the details and the whole ingredient list. The author, or an owner or admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the author or an owner or admin can change it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Recipe or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{recipeId}")
    RecipeDTO replaceRecipe(@PathVariable UUID workspaceId, @PathVariable UUID recipeId,
            @Valid @RequestBody SaveRecipeRequest request) {
        return recipeService.replaceRecipe(workspaceId, recipeId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/recipes/{recipeId}: planned meals keep their title. */
    @Operation(summary = "Delete a recipe",
            description = "The author, or an owner or admin. Meals already planned with it keep their title.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Recipe deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the author or an owner or admin can delete it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Recipe or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{recipeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRecipe(@PathVariable UUID workspaceId, @PathVariable UUID recipeId) {
        recipeService.deleteRecipe(workspaceId, recipeId);
    }

    /** POST /api/v1/workspaces/{id}/recipes/{recipeId}/shopping-list: adds every ingredient to a list. */
    @Operation(summary = "Add the ingredients to a shopping list",
            description = "Adds every ingredient as an item of a list in the same workspace. The other members are "
                    + "told once per list, as for items added by hand.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The items added"),
            @ApiResponse(responseCode = "400", description = "The recipe has no ingredients", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Recipe, list or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The list is archived", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{recipeId}/shopping-list")
    @ResponseStatus(HttpStatus.CREATED)
    List<ShoppingItemDTO> addToShoppingList(@PathVariable UUID workspaceId, @PathVariable UUID recipeId,
            @Valid @RequestBody AddToShoppingListRequest request) {
        return recipeService.addToShoppingList(workspaceId, recipeId, request);
    }
}
