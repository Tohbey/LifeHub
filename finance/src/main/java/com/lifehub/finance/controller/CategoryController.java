package com.lifehub.finance.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.finance.model.dto.CategoryDTO;
import com.lifehub.finance.model.dto.CreateCategoryRequest;
import com.lifehub.finance.model.dto.UpdateCategoryRequest;
import com.lifehub.finance.service.CategoryService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Categories", description = "Income and expense categories of a workspace.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/finance/categories")
@RequiredArgsConstructor
class CategoryController {

    private final CategoryService categoryService;

    /** GET /api/v1/workspaces/{id}/finance/categories: the workspace's categories, by name. */
    @Operation(summary = "List categories", description = "The workspace's income and expense categories, by name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of categories"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<CategoryDTO> getCategories(@PathVariable UUID workspaceId,
            @PageableDefault(size = 100, sort = "name") Pageable pageable) {
        return categoryService.getCategories(workspaceId, pageable);
    }

    /** POST /api/v1/workspaces/{id}/finance/categories: adds a category. */
    @Operation(summary = "Add a category", description = "Names are unique per workspace and type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category added",
                    content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "A category with this name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryDTO createCategory(@PathVariable UUID workspaceId, @Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(workspaceId, request);
    }

    /** PATCH /api/v1/workspaces/{id}/finance/categories/{categoryId}: renames a category. Owners and admins only. */
    @Operation(summary = "Rename a category", description = "Owners and admins only. The type cannot change.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category renamed",
                    content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only owners and admins can rename categories",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Category or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "A category with this name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{categoryId}")
    CategoryDTO renameCategory(@PathVariable UUID workspaceId, @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.renameCategory(workspaceId, categoryId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/finance/categories/{categoryId}: deletes an unused category. Returns 204. */
    @Operation(summary = "Delete a category",
            description = "Owners and admins only. A category used by any transaction or budget cannot be deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only owners and admins can delete categories",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Category or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The category is in use",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{categoryId}")
    ResponseEntity<Void> deleteCategory(@PathVariable UUID workspaceId, @PathVariable UUID categoryId) {
        categoryService.deleteCategory(workspaceId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
