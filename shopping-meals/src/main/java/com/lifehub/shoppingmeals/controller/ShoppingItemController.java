package com.lifehub.shoppingmeals.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.shoppingmeals.model.dto.CreateShoppingItemRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.shoppingmeals.model.dto.UpdateShoppingItemRequest;
import com.lifehub.shoppingmeals.service.ShoppingItemService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Item endpoints are keyed by list or item ID (README section 11); the service checks the workspace. */
@RestController
@Tag(name = "Shopping items", description = "Items on a list: add, edit, tick off and remove.")
@RequiredArgsConstructor
class ShoppingItemController {

    private final ShoppingItemService itemService;

    /** GET /api/v1/shopping-lists/{listId}/items: items in the order added; {@code ?open=true} hides bought ones. */
    @Operation(summary = "List items on a list",
            description = "In the order they were added. Use open=true to hide bought items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of items"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Shopping list not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/shopping-lists/{listId}/items")
    PageResponse<ShoppingItemDTO> getItems(@PathVariable UUID listId,
            @RequestParam(defaultValue = "false") boolean open,
            @PageableDefault(size = 100, sort = "createdAt") Pageable pageable) {
        return itemService.getItems(listId, open, pageable);
    }

    /** POST /api/v1/shopping-lists/{listId}/items: adds an item. */
    @Operation(summary = "Add an item to a list", description = "Notifies the other members (grouped per list).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item added",
                    content = @Content(schema = @Schema(implementation = ShoppingItemDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Shopping list not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The list is archived",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/api/v1/shopping-lists/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    ShoppingItemDTO addItem(@PathVariable UUID listId, @Valid @RequestBody CreateShoppingItemRequest request) {
        return itemService.addItem(listId, request);
    }

    /** PATCH /api/v1/shopping-items/{itemId}: edits an item, or ticks it off with {@code purchased: true}. */
    @Operation(summary = "Update or tick off an item",
            description = "Edits the item; purchased=true ticks it off, false puts it back on the list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item updated",
                    content = @Content(schema = @Schema(implementation = ShoppingItemDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Shopping item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/api/v1/shopping-items/{itemId}")
    ShoppingItemDTO updateItem(@PathVariable UUID itemId, @Valid @RequestBody UpdateShoppingItemRequest request) {
        return itemService.updateItem(itemId, request);
    }

    /** DELETE /api/v1/shopping-items/{itemId}: removes an item. Returns 204. */
    @Operation(summary = "Remove an item", description = "Deletes the item from its list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item removed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Shopping item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/api/v1/shopping-items/{itemId}")
    ResponseEntity<Void> removeItem(@PathVariable UUID itemId) {
        itemService.removeItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
