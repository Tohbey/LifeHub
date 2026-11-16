package com.lifehub.finance.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.finance.model.dto.CategoryDTO;
import com.lifehub.finance.model.dto.CreateCategoryRequest;
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
}
