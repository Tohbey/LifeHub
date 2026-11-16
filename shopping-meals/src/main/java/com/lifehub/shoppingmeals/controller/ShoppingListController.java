package com.lifehub.shoppingmeals.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.shoppingmeals.model.dto.CreateShoppingListRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingListDTO;
import com.lifehub.shoppingmeals.model.dto.UpdateShoppingListRequest;
import com.lifehub.shoppingmeals.service.ShoppingListService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Shopping lists", description = "A workspace's shopping lists.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/shopping-lists")
@RequiredArgsConstructor
class ShoppingListController {

    private final ShoppingListService listService;

    /** GET /api/v1/workspaces/{id}/shopping-lists: lists in use, newest first; {@code ?includeArchived=true}. */
    @Operation(summary = "List shopping lists",
            description = "Lists in use, newest first, with how many items are still to buy. Use includeArchived=true "
                    + "for archived ones too.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of lists"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<ShoppingListDTO> getLists(@PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return listService.getLists(workspaceId, includeArchived, pageable);
    }

    /** POST /api/v1/workspaces/{id}/shopping-lists: creates a list. */
    @Operation(summary = "Create a shopping list", description = "Any member can create lists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "List created",
                    content = @Content(schema = @Schema(implementation = ShoppingListDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShoppingListDTO createList(@PathVariable UUID workspaceId, @Valid @RequestBody CreateShoppingListRequest request) {
        return listService.createList(workspaceId, request);
    }

    /** GET /api/v1/workspaces/{id}/shopping-lists/{listId}: one list. */
    @Operation(summary = "Get a shopping list", description = "One list with its open-item count.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The list",
                    content = @Content(schema = @Schema(implementation = ShoppingListDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "List or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{listId}")
    ShoppingListDTO getList(@PathVariable UUID workspaceId, @PathVariable UUID listId) {
        return listService.getList(workspaceId, listId);
    }

    /** PATCH /api/v1/workspaces/{id}/shopping-lists/{listId}: renames, archives or restores a list. */
    @Operation(summary = "Rename, archive or restore a list", description = "Changes the given fields.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List updated",
                    content = @Content(schema = @Schema(implementation = ShoppingListDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "List or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{listId}")
    ShoppingListDTO updateList(@PathVariable UUID workspaceId, @PathVariable UUID listId,
            @Valid @RequestBody UpdateShoppingListRequest request) {
        return listService.updateList(workspaceId, listId, request);
    }
}
