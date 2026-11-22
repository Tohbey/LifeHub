package com.lifehub.shoppingmeals.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.shoppingmeals.model.dto.CreateMealPlanEntryRequest;
import com.lifehub.shoppingmeals.model.dto.MealPlanEntryDTO;
import com.lifehub.shoppingmeals.service.MealPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Meal plan", description = "What the household eats, day by day.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/meals")
@RequiredArgsConstructor
class MealPlanController {

    private final MealPlanService mealPlanService;

    /** GET /api/v1/workspaces/{id}/meals?from=&to=: the plan for a range of days, at most 62. */
    @Operation(summary = "Get the meal plan",
            description = "Meals from 'from' to 'to' (YYYY-MM-DD, both included, at most 62 days), by day and then "
                    + "breakfast, lunch, dinner and snack.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The planned meals"),
            @ApiResponse(responseCode = "400", description = "The range is backwards or longer than 62 days", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    List<MealPlanEntryDTO> getPlan(@PathVariable UUID workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return mealPlanService.getPlan(workspaceId, from, to);
    }

    /** POST /api/v1/workspaces/{id}/meals: plans a recipe or a free-text meal. */
    @Operation(summary = "Plan a meal", description = "A recipe of this workspace, or a free-text title such as "
            + "\"Takeaway\" when recipeId is empty. Any member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Meal planned",
                    content = @Content(schema = @Schema(implementation = MealPlanEntryDTO.class))),
            @ApiResponse(responseCode = "400", description = "No recipe or title, or a recipe from another workspace", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MealPlanEntryDTO addEntry(@PathVariable UUID workspaceId, @Valid @RequestBody CreateMealPlanEntryRequest request) {
        return mealPlanService.addEntry(workspaceId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/meals/{entryId}. */
    @Operation(summary = "Remove a planned meal", description = "Any member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Meal removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Planned meal or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteEntry(@PathVariable UUID workspaceId, @PathVariable UUID entryId) {
        mealPlanService.deleteEntry(workspaceId, entryId);
    }
}
